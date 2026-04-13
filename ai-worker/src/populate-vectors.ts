import { createWorkersAI } from 'workers-ai-provider';
import { Vectorize } from '@cloudflare/workers-types';

export interface Env {
  AI: Fetcher;
  VECTORIZE: Vectorize;
  VECTORIZE_FAQ: Vectorize;
  SUPABASE_URL: string;
  SUPABASE_ANON_KEY: string;
}

async function generateEmbedding(text: string, ai: any): Promise<number[]> {
  const response = await ai.run('@cf/baai/bge-m3', { text: [text] });
  return response.data[0];
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);
    
    if (url.pathname === '/populate-vectors') {
      return await this.populateVectors(env);
    }
    
    return new Response('Use /populate-vectors to populate vector database', { status: 404 });
  },
  
  async populateVectors(env: Env): Promise<Response> {
    try {
      const supabaseUrl = env.SUPABASE_URL || 'https://plqzygsrozwylyelhzue.supabase.co';
      const supabaseKey = env.SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBscXp5Z3Nyb3p3eWx5ZWxoenVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIyMDczNTAsImV4cCI6MjA4Nzc4MzM1MH0.5HBjueHX9VCzux-NuLHuOLFClKIqa8NSiwi-V9pDRQ0';
      
      const carsResponse = await fetch(`${supabaseUrl}/rest/v1/cars?select=*`, {
        headers: {
          'apikey': supabaseKey,
          'Authorization': `Bearer ${supabaseKey}`
        }
      });
      const cars = await carsResponse.json();
      
      const carVectors = [];
      for (const car of cars) {
        const text = `${car.make} ${car.model} ${car.year} ${car.body_style || ''} ${car.fuel_type || ''} ${car.transmission || ''}`.trim();
        const embedding = await generateEmbedding(text, env.AI);
        
        carVectors.push({
          id: String(car.id),
          values: embedding,
          metadata: {
            name: car.name,
            make: car.make,
            model: car.model,
            year: car.year,
            price: car.price,
            category: car.body_style,
            image_url: car.image_url
          }
        });
      }
      
      await env.VECTORIZE.upsert(carVectors);
      
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
      for (const faq of faqs) {
        const text = `${faq.question} ${faq.answer}`.trim();
        const embedding = await generateEmbedding(text, env.AI);
        
        faqVectors.push({
          id: String(faqVectors.length + 1),
          values: embedding,
          metadata: {
            question: faq.question,
            answer: faq.answer
          }
        });
      }
      
      await env.VECTORIZE_FAQ.upsert(faqVectors);
      
      return Response.json({
        success: true,
        carsInserted: carVectors.length,
        faqsInserted: faqVectors.length
      });
    } catch (error) {
      return Response.json({ error: String(error) }, { status: 500 });
    }
  }
} satisfies ExportedHandler<Env>;
