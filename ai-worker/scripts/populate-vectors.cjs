const https = require('https');

const CLOUDFLARE_API_TOKEN = 'vZXnNCEkZDLXWTjtqT86zkyxEAI1TP4lJfOWHeEN';
const ACCOUNT_ID = 'cf5f13bd5550ea98e75fea6f3ddd321b';

const SUPABASE_URL = 'https://plqzygsrozwylyelhzue.supabase.co';
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBscXp5Z3Nyb3p3eWx5ZWxoenVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIyMDczNTAsImV4cCI6MjA4Nzc4MzM1MH0.5HBjueHX9VCzux-NuLHuOLFClKIqa8NSiwi-V9pDRQ0';

async function fetchWithTimeout(url, options = {}, timeout = 60000) {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);
  
  try {
    const response = await fetch(url, { ...options, signal: controller.signal });
    clearTimeout(id);
    return response;
  } catch (e) {
    clearTimeout(id);
    throw e;
  }
}

async function generateEmbedding(text) {
  const requestBody = JSON.stringify({
    text: [text]
  });
  
  console.log(`Generating embedding for: ${text.substring(0, 50)}...`);
  
  const response = await fetchWithTimeout(
    `https://api.cloudflare.com/client/v4/accounts/${ACCOUNT_ID}/ai/run/@cf/baai/bge-m3`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${CLOUDFLARE_API_TOKEN}`,
        'Content-Type': 'application/json'
      },
      body: requestBody
    }
  );
  
  const data = await response.json();
  console.log('API Response:', JSON.stringify(data).substring(0, 200));
  
  if (!data.success) {
    throw new Error(`AI API error: ${JSON.stringify(data.errors)}`);
  }
  
  return data.result.data[0];
}

async function getCars() {
  const response = await fetchWithTimeout(`${SUPABASE_URL}/rest/v1/cars?select=*`, {
    headers: {
      'apikey': SUPABASE_KEY,
      'Authorization': `Bearer ${SUPABASE_KEY}`
    }
  });
  return response.json();
}

async function insertVectors(indexName, vectors) {
  console.log(`Inserting ${vectors.length} vectors to ${indexName}...`);
  
  const { execSync } = require('child_process');
  
  const tempFile = `temp_${indexName}_${Date.now()}.jsonl`;
  const fs = require('fs');
  
  const jsonlContent = vectors.map(v => JSON.stringify(v)).join('\n');
  fs.writeFileSync(tempFile, jsonlContent);
  
  try {
    const cmd = `npx wrangler vectorize insert ${indexName} --file=${tempFile}`;
    console.log('Running:', cmd);
    const result = execSync(cmd, { encoding: 'utf8', stdio: 'pipe' });
    console.log('Result:', result);
    return { success: true, result };
  } catch (e) {
    console.error('Error:', e.message);
    return { success: false, error: e.message };
  } finally {
    fs.unlinkSync(tempFile);
  }
}

async function main() {
  console.log('Fetching cars from Supabase...');
  const cars = await getCars();
  console.log(`Found ${cars.length} cars`);
  
  console.log('Generating car embeddings...');
  const carVectors = [];
  
  for (let i = 0; i < cars.length; i++) {
    const car = cars[i];
    const text = `${car.make} ${car.model} ${car.year} ${car.body_style || ''} ${car.fuel_type || ''} ${car.transmission || ''}`.trim();
    
    try {
      const embedding = await generateEmbedding(text);
      
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
      
      console.log(`Processed car ${i + 1}/${cars.length}: ${car.name}`);
    } catch (e) {
      console.error(`Error processing car ${car.id}:`, e.message);
    }
  }
  
  console.log(`Inserting ${carVectors.length} car vectors...`);
  const result = await insertVectors('rentacar-cars', carVectors);
  console.log('Car vectors inserted:', result);
  
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
  
  console.log('Generating FAQ embeddings...');
  const faqVectors = [];
  
  for (let i = 0; i < faqs.length; i++) {
    const faq = faqs[i];
    const text = `${faq.question} ${faq.answer}`.trim();
    
    try {
      const embedding = await generateEmbedding(text);
      
      faqVectors.push({
        id: String(i + 1),
        values: embedding,
        metadata: {
          question: faq.question,
          answer: faq.answer
        }
      });
      
      console.log(`Processed FAQ ${i + 1}/${faqs.length}: ${faq.question.substring(0, 30)}...`);
    } catch (e) {
      console.error(`Error processing FAQ ${i}:`, e.message);
    }
  }
  
  console.log(`Inserting ${faqVectors.length} FAQ vectors...`);
  const faqResult = await insertVectors('rentacar-faq', faqVectors);
  console.log('FAQ vectors inserted:', faqResult);
  
  console.log('\nDone!');
  console.log(`Total car vectors: ${carVectors.length}`);
  console.log(`Total FAQ vectors: ${faqVectors.length}`);
}

main().catch(console.error);
