/// <reference types="@cloudflare/workers-types" />
import { createWorkersAI } from 'workers-ai-provider';
import { generateText, tool } from 'ai';
import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { analyzeSleep, validateSleepRecord, type SleepRecord } from './sleep-analyzer';
import { generateReport, generateMarkdownReport } from './sleep-report';

// ── Types ─────────────────────────────────────────────────────────────────────

type Env = {
  AI: Fetcher;
  VECTORIZE_FAQ: VectorizeIndex;
  SUPABASE_URL: string;
  SUPABASE_ANON_KEY: string;
  SUPABASE_SERVICE_ROLE_KEY: string;
};

// ── Logging ───────────────────────────────────────────────────────────────────

type LogLevel = 'info' | 'warn' | 'error';
function log(level: LogLevel, event: string, fields: Record<string, unknown> = {}): void {
  const entry = { ts: new Date().toISOString(), level, service: 'ai-worker', event, ...fields };
  level === 'error' ? console.error(JSON.stringify(entry)) : console.log(JSON.stringify(entry));
}

// ── Text utilities ─────────────────────────────────────────────────────────────

function normalizeCarNames(text: string): string {
  if (!text) return text;
  let normalized = text;

  const brandMappings: Record<string, string> = {
    'toyota': 'Toyota', 'ford': 'Ford', 'honda': 'Honda', 'hyundai': 'Hyundai',
    'kia': 'Kia', 'mazda': 'Mazda', 'nissan': 'Nissan', 'chevrolet': 'Chevrolet',
    'bmw': 'BMW', 'mercedes': 'Mercedes-Benz', 'audi': 'Audi', 'volkswagen': 'Volkswagen',
    'mitsubishi': 'Mitsubishi', 'suzuki': 'Suzuki', 'lexus': 'Lexus', 'porsche': 'Porsche',
    'land rover': 'Land Rover', 'jaguar': 'Jaguar', 'volvo': 'Volvo', 'tesla': 'Tesla',
  };

  const modelMappings: Record<string, string> = {
    'mười chín': '19', 'mười tám': '18', 'mười bảy': '17', 'mười sáu': '16',
    'mười lăm': '15', 'mười bốn': '14', 'mười ba': '13', 'mười hai': '12',
    'mười một': '11', 'mười': '10', 'chín': '9', 'tám': '8', 'bảy': '7',
    'sáu': '6', 'năm': '5', 'tư': '4', 'ba': '3', 'hai': '2', 'một': '1',
  };

  const phraseMappings: Record<string, string> = {
    // Selection/ordinal phrases - normalize to "xe thứ X" pattern
    'chiếc đầu tiên': 'xe thứ 1', 'chiếc thứ nhất': 'xe thứ 1', 'đầu tiên': 'xe thứ 1',
    'chiếc thứ hai': 'xe thứ 2', 'chiếc thứ 2': 'xe thứ 2', 'thứ hai': 'xe thứ 2',
    'chiếc thứ ba': 'xe thứ 3', 'chiếc thứ 3': 'xe thứ 3', 'thứ ba': 'xe thứ 3',
    'chiếc thứ tư': 'xe thứ 4', 'chiếc thứ 4': 'xe thứ 4', 'thứ tư': 'xe thứ 4',
    'chiếc thứ năm': 'xe thứ 5', 'chiếc thứ 5': 'xe thứ 5', 'thứ năm': 'xe thứ 5',
    'chiếc cuối cùng': 'xe cuối cùng', 'cuối cùng': 'xe cuối cùng',
    // Fix STT misrecognitions
    'đầu xe': 'đầu tiên',
  };

  // Sort by length descending so longer patterns match before shorter ones
  const sortedBrands = Object.entries(brandMappings).sort((a, b) => b[0].length - a[0].length);
  for (const [vn, en] of sortedBrands) {
    normalized = normalized.replace(new RegExp('\\b' + vn + '\\b', 'gi'), en);
  }
  for (const [vn, en] of Object.entries(modelMappings)) {
    normalized = normalized.replace(new RegExp('\\b' + vn + '\\b', 'gi'), en);
  }
  for (const [wrong, correct] of Object.entries(phraseMappings)) {
    normalized = normalized.replace(new RegExp(wrong, 'gi'), correct);
  }

  normalized = normalized.replace(/\b(x)\s+(\d+)\b/gi, '$1$2');
  normalized = normalized.replace(/\b(s)\s+(\d+)\b/gi, '$1$2');
  return normalized;
}

async function generateEmbedding(text: string, env: Env): Promise<number[]> {
  try {
    const response = await (env.AI as any).run('@cf/baai/bge-m3', { text: [text] });
    return response.data[0];
  } catch (e) {
    console.error('Embedding error:', e);
    return [];
  }
}

// ── MCP Server Initialization & Transport (Hono) ──────────────────────────────
const app = new Hono<{ Bindings: Env }>();
app.use('*', cors({ origin: '*' }));

// Observability
app.use('*', async (c, next) => {
  const start = Date.now();
  const reqId = Math.random().toString(36).slice(2, 10);
  c.header('X-Request-Id', reqId);
  await next();
  const path = new URL(c.req.url).pathname;
  if (path !== '/health') {
    log('info', 'request', { request_id: reqId, method: c.req.method, path, status: c.res.status, latency_ms: Date.now() - start });
  }
});

app.get('/health', (c) => c.json({ status: 'ok', service: 'ai-worker', version: '1.0.0', ts: new Date().toISOString() }));

// ── STT ───────────────────────────────────────────────────────────────────────
app.post('/stt', async (c) => {
  try {
    const { audio_base64 } = await c.req.json();
    if (!audio_base64) return c.json({ error: 'Missing audio data (audio_base64)' }, 400);
    const response = await (c.env.AI as any).run('@cf/openai/whisper-large-v3-turbo', {
      audio: audio_base64, language: 'en',
      initial_prompt: 'Rent A Car - car rental service. Car brands: Toyota, Ford, Honda, Hyundai, Kia, Mazda, Nissan, Chevrolet, BMW, Mercedes-Benz, Audi, Volkswagen.',
    });
    return c.json({ text: normalizeCarNames(response.text || '') });
  } catch (error: any) {
    log('error', 'stt.error', { message: error.message });
    return c.json({ error: error.message }, 500);
  }
});

// ── TTS ───────────────────────────────────────────────────────────────────────
app.post('/tts', async (c) => {
  try {
    const { text, lang } = await c.req.json();
    if (!text) return c.json({ error: 'Missing text content' }, 400);
    const response = await (c.env.AI as any).run('@cf/myshell-ai/melotts', { 
      prompt: text,
      lang: 'en'
    });
    const audioBase64 = (response as any).audio || response;
    return c.json({ audio_base64: audioBase64 });
  } catch (error: any) {
    return c.json({ error: error.message }, 500);
  }
});

// ── Embeddings ────────────────────────────────────────────────────────────────
app.post('/embed', async (c) => {
  try {
    const { text } = await c.req.json();
    if (!text) return c.json({ error: 'Missing text' }, 400);
    const response = await (c.env.AI as any).run('@cf/baai/bge-m3', { text: [text] });
    return c.json({ embedding: response.data[0] });
  } catch (error: any) {
    return c.json({ error: error.message }, 500);
  }
});

// ── Populate Vectorize (Admin endpoint) ────────────────────────────────────────
app.post('/admin/populate-vectors', async (c) => {
  try {
    const env = c.env;

    const faqs = [
      { question: "How do I rent a car?", answer: "You can rent a car by using our app or website. Search for your desired vehicle, select the pickup and return dates, and complete the booking." },
      { question: "What documents do I need?", answer: "You'll need a valid driver's license, a credit card for payment and security deposit, and proof of identity." },
      { question: "What is the minimum age?", answer: "The minimum age to rent a car is typically 21 years old. Young driver fees may apply for drivers under 25." },
      { question: "Can I cancel my rental?", answer: "Yes, free cancellation is available up to 24 hours before pickup." },
      { question: "What is the deposit amount?", answer: "A security deposit is required at pickup, typically ranging from $200 to $500 depending on the vehicle type." },
      { question: "Is insurance included?", answer: "Basic insurance is included with all rentals. You can purchase additional coverage." },
      { question: "What if I return the car late?", answer: "Late returns are charged at an hourly rate. A full additional day rate may apply after 2 hours." },
      { question: "Can I add an additional driver?", answer: "Yes, additional drivers can be added for a small daily fee." },
      { question: "What is the fuel policy?", answer: "We offer full-to-full fuel policy - you receive and return the car with a full tank." },
      { question: "Can I rent one way?", answer: "Yes, one-way rentals are available between most locations." },
      { question: "What to do in case of accident?", answer: "Contact emergency services if needed, then call our 24/7 support line." },
      { question: "Can I extend my rental?", answer: "Yes, contact us before your scheduled return time to extend." },
      { question: "Do you have electric vehicles?", answer: "Yes, we have Tesla, Hyundai Ioniq, and other EV models." },
      { question: "What if car breaks down?", answer: "All vehicles include 24/7 roadside assistance." },
      { question: "How to find rental location?", answer: "After booking, you'll receive detailed directions to the pickup location." }
    ];
    
    const faqVectors = [];
    for (let i = 0; i < faqs.length; i++) {
      const faq = faqs[i];
      const text = `${faq.question} ${faq.answer}`.trim();
      const response = await (env.AI as any).run('@cf/baai/bge-m3', { text: [text] });
      const embedding = response.data[0];
      
      faqVectors.push({
        id: String(i + 1),
        values: embedding,
        metadata: {
          question: faq.question,
          answer: faq.answer
        }
      });
    }
    
    await env.VECTORIZE_FAQ.upsert(faqVectors);
    
    return c.json({
      success: true,
      faqsInserted: faqVectors.length
    });
  } catch (error: any) {
    return c.json({ error: error.message }, 500);
  }
});

// ── Voice process (STT → LLM → tool execution → TTS) ────────────────────────
app.post('/voice-process', async (c) => {
  try {
    const { text: inputText, audio_base64, session_id, context } = await c.req.json();
    const userId: string | null = context?.user_id || null;
    const lastSearchResults: Array<{id: string; name: string; price: number; index: number}> = context?.last_search_results || [];
    const conversationHistory: Array<{role: 'user' | 'assistant'; content: string}> = context?.conversation_history || [];
    
    const env = c.env;
    const workersai = createWorkersAI({ binding: env.AI as any });
    
    let userText = inputText || '';
    
    // STT: if audio provided, transcribe it
    if (!userText && audio_base64) {
      try {
        const stt = await (env.AI as any).run('@cf/openai/whisper-large-v3-turbo', {
          audio: audio_base64,
          language: 'en',
          initial_prompt: 'Rent A Car - car rental service. Car brands: Toyota, Ford, Honda, Hyundai, Kia, Mazda, Nissan, Chevrolet, BMW, Mercedes-Benz, Audi, Volkswagen.'
        });
        userText = normalizeCarNames(stt.text || '');
      } catch (sttErr) {
        console.error('STT error:', sttErr);
      }
    }
    
    if (!userText.trim()) {
      return c.json({ response_text: 'Sorry, I did not hear you clearly.' });
    }
    
    const { createRentCarMcpServer } = await import('./mcp');
    const { createMCPClient } = await import('@ai-sdk/mcp');
    const { InMemoryTransport } = await import('@modelcontextprotocol/sdk/inMemory.js');
    
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    
    const mcpServer = createRentCarMcpServer({
      AI: env.AI,
      VECTORIZE_FAQ: env.VECTORIZE_FAQ,
      SUPABASE_URL: env.SUPABASE_URL,
      SUPABASE_ANON_KEY: env.SUPABASE_ANON_KEY,
      SUPABASE_SERVICE_ROLE_KEY: env.SUPABASE_SERVICE_ROLE_KEY,
    }, lastSearchResults, userId || '');
    await mcpServer.connect(serverTransport);
    
    const mcpClient = await createMCPClient({ transport: clientTransport as any });
    const mcpTools = await mcpClient.tools();
    
    // Build car context for the LLM
    let carContext = '';
    if (lastSearchResults.length > 0) {
      carContext = `
## Previously shown cars:
${lastSearchResults.map((p: any) => `${p.index}. ${p.name} - $${p.price?.toLocaleString('en-US') || 'N/A'}/day (ID: ${p.id})`).join('\n')}
 
IMPORTANT:
- When user says "car number X", "the first one" (X=1), "the second one" (X=2), "the last one", ALWAYS use the corresponding carId from the list above (ID: ${lastSearchResults.map((p: any) => p.id).join(' or ')}).
- "the first one" = car number 1 (index 1)
- "the second one" = car number 2 (index 2)  
- "the third one" = car number 3 (index 3)
- "the last one" = last car in the list
- NEVER skip or make up carId - always use the actual ID from the list above.
- If user asks about specs, features, or details of a specific car (e.g., "what engine does Toyota Camry have?", "what's the fuel efficiency?"), call getCarDetails with the carName to get information.
- If user just says "car number X" or car name WITHOUT an action verb (like "rent", "view", "cancel"), call getCarDetails with the corresponding carId to show info, then ask what they want to do next.`;
    }
    
    const userIdValue = userId || '';
    const ctxJson = JSON.stringify({ current_user_id: userIdValue });
    const systemPrompt = `You are Rent A Car AI — an intelligent voice assistant for Rent A Car, a leading car rental service.
You communicate primarily in English. Keep responses brief (1-3 sentences). Always use tools for car rental actions — never make up data.

IMPORTANT - Conversational Rental Flow:
When user wants to rent a car (e.g., "I want to rent a car", "rent me a car", "book a car"), you MUST use the "rentalFlow" tool to guide them through these steps:
1. Start: Call rentalFlow with action="start" to begin the rental process
2. Select Car: Ask user which car they want, then call rentalFlow with action="select_car" and the carName/carId
3. Set Days: Ask "How many days?" (1-7 days max), then call rentalFlow with action="set_days" and the number of days
4. Confirm: Tell user the total price and ask "Should I confirm?" Then call rentalFlow with action="confirm" if they say yes, or "cancel" if they say no

Example conversation:
- User: "I want to rent a car" → You: "Sure! Which car would you like?" → Call rentalFlow(action="start")
- User: "Toyota Camry" → You: "Great choice! How many days?" → Call rentalFlow(action="select_car", carName="Toyota Camry")
- User: "3 days" → You: "That's $840 total. Should I confirm?" → Call rentalFlow(action="set_days", days=3)
- User: "yes" → Call rentalFlow(action="confirm")

Available actions:
- rentalFlow: For conversational rental flow (start, select_car, set_days, confirm, cancel, status)
- searchCars: When user wants to find cars, asks about available cars
- getCarDetails: When user asks about specs, features of a specific car
- viewRentals: When user wants to see rental history or status
- cancelRental: When user wants to cancel a car rental

When user asks about their rentals, get current_user_id from this JSON and pass to userId parameter: ${ctxJson}
${conversationHistory.length > 0 ? `\n\n## Recent conversation:\n${conversationHistory.map(h => `${h.role === 'user' ? 'User' : 'AI'}: ${h.content}`).join('\n')}` : ''}${carContext}`;
    
    const messages = [{ role: 'user', content: userText }];
    
    const result = await generateText({
      model: workersai('@cf/mistralai/mistral-small-3.1-24b-instruct') as any,
      system: systemPrompt,
      messages: messages as any,
      tools: mcpTools as any,
    });
    
    let responseText = result.text;
    if (!responseText && (result as any).toolResults?.length) {
      const toolResults = (result as any).toolResults;
      const lastTool = toolResults[toolResults.length - 1];
      if (lastTool.output?.content?.[0]?.text) {
        try {
          const parsed = JSON.parse(lastTool.output.content[0].text);
          responseText = parsed.message || parsed.answer || '';
        } catch {
          responseText = lastTool.output.content[0].text;
        }
      }
    }
    
    // TTS: Generate audio from response (English via melotts)
    let audioBase64 = null;
    if (responseText) {
      try {
        const tts = await (env.AI as any).run('@cf/myshell-ai/melotts', {
          prompt: responseText.slice(0, 500),
          lang: 'en'
        });
        audioBase64 = (tts as any).audio || tts;
      } catch (ttsErr) {
        console.error('TTS error:', ttsErr);
      }
    }

    // Detect intent from tool calls for logging
    let intent: string | null = null;
    if ((result as any).toolCalls?.length) {
      intent = (result as any).toolCalls[0]?.toolName || null;
    } else if (result.toolResults?.length) {
      intent = (result.toolResults[0] as any)?.toolName || null;
    }
    
    // Detect action for frontend
    const { processCarIntent } = await import('./intent');
    const intentResult = processCarIntent(result.toolResults || [], userText);
    const action = intentResult.action;
    const searchResults = intentResult.searchResults;
    const rentalInfo = intentResult.rentalInfo;

    return c.json({
      transcribed_text: userText,
      response_text: responseText,
      audio_base64: audioBase64,
      tool_results: result.toolResults,
      action,
      search_results: searchResults,
      rental_info: rentalInfo,
      session_id: session_id || 'default'
    });
  } catch (error: any) {
    return c.json({ error: error.message }, 500);
  }
});

// ── Snore Detection ─────────────────────────────────────────────────────────────
app.post('/snore/detect', async (c) => {
  try {
    const body = await c.req.json();
    const { audio_base64, audio_chunks, sample_rate, chunk_index, total_chunks } = body;
    
    if (!audio_base64 && (!audio_chunks || audio_chunks.length === 0)) {
      return c.json({ error: 'Missing audio data (audio_base64 or audio_chunks)' }, 400);
    }
    
    const env = c.env;
    const options = { sampleRate: sample_rate || 16000 };
    
    let result;
    if (audio_chunks && audio_chunks.length > 0) {
      const { detectSnoringFromChunks } = await import('./snore-detector');
      result = await detectSnoringFromChunks(audio_chunks, { AI: env.AI }, options);
    } else {
      const { detectSnoring } = await import('./snore-detector');
      result = await detectSnoring(audio_base64, { AI: env.AI }, {
        sampleRate: options.sampleRate,
        chunkIndex: chunk_index,
        totalChunks: total_chunks
      });
    }
    
    return c.json(result);
  } catch (error: any) {
    log('error', 'snore.detect.error', { message: error.message });
    return c.json({ error: error.message }, 500);
  }
});

// ── Sleep Analysis ─────────────────────────────────────────────────────────────
app.post('/sleep/analyze', async (c) => {
  try {
    const record = await c.req.json() as SleepRecord;

    const validation = validateSleepRecord(record);
    if (!validation.valid) {
      log('warn', 'sleep.analyze.validation', { error: validation.error });
      return c.json({ error: validation.error }, 400);
    }

    const useAIEnhancement = record.events.length > 10;

    const analysis = await analyzeSleep(record, c.env as Env, useAIEnhancement);
    const report = generateReport(record, analysis);

    const acceptHeader = c.req.header('Accept') || '';
    if (acceptHeader.includes('text/markdown')) {
      const markdown = generateMarkdownReport(record, analysis);
      return c.json({
        analysis,
        report,
        markdown,
      });
    }

    log('info', 'sleep.analyze.success', {
      user_id: record.user_id,
      date: record.date,
      quality_score: analysis.quality_score,
      use_ai: useAIEnhancement,
    });

    return c.json({
      analysis,
      report,
    });
  } catch (error: any) {
    log('error', 'sleep.analyze.error', { message: error.message });
    return c.json({ error: error.message }, 500);
  }
});

// Cloudflare Workers entry point
export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    return app.fetch(request, env, ctx);
  },
};
