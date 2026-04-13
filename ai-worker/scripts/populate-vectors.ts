// Script to populate Vectorize indexes with car and FAQ data
// Run with: npx wrangler exec script.ts

interface Car {
  id: number;
  name: string;
  make: string;
  model: string;
  year: number;
  price: number;
  body_style: string;
  fuel_type: string;
  transmission: string;
  engine: string;
  mpg_city: number;
  mpg_highway: number;
  image_url: string;
}

interface FAQ {
  question: string;
  answer: string;
}

// Car rental FAQs
const faqs: FAQ[] = [
  {
    question: "How do I rent a car?",
    answer: "You can rent a car by using our app or website. Search for your desired vehicle, select the pickup and return dates, and complete the booking. You'll need a valid driver's license and credit card."
  },
  {
    question: "What documents do I need to rent a car?",
    answer: "You'll need a valid driver's license, a credit card or debit card for payment and security deposit, and proof of identity (passport or national ID)."
  },
  {
    question: "What is the minimum age to rent a car?",
    answer: "The minimum age to rent a car is typically 21 years old. Some vehicle categories may require drivers to be 25 or older. Young driver fees may apply for drivers under 25."
  },
  {
    question: "Can I cancel my car rental?",
    answer: "Yes, you can cancel your rental. Free cancellation is available up to 24 hours before pickup. Late cancellations may incur a fee equal to one day's rental."
  },
  {
    question: "What is the deposit amount?",
    answer: "A security deposit is required at pickup, typically ranging from $200 to $500 depending on the vehicle type. The deposit is refunded upon return if the car is in good condition."
  },
  {
    question: "Is insurance included?",
    answer: "Basic insurance is included with all rentals. You can purchase additional coverage including collision damage waiver, personal accident insurance, and supplemental liability protection."
  },
  {
    question: "What happens if I return the car late?",
    answer: "Late returns are charged at an hourly rate. If you return more than 2 hours late, a full additional day rate may apply. Please contact us if you need an extension."
  },
  {
    question: "Can I add an additional driver?",
    answer: "Yes, additional drivers can be added for a small daily fee. Each additional driver must meet the same age requirements and provide their driver's license."
  },
  {
    question: "What is the fuel policy?",
    answer: "We offer both full-to-full and prepaid fuel options. With full-to-full, you receive the car with a full tank and return it full to avoid refueling charges."
  },
  {
    question: "Can I rent a car one way?",
    answer: "Yes, one-way rentals are available between most locations. One-way drop-off fees may apply depending on the pickup and return locations."
  },
  {
    question: "What should I do in case of an accident?",
    answer: "In case of an accident, immediately contact emergency services if needed, then call our 24/7 support line. Take photos of the damage and get a police report if required."
  },
  {
    question: "Can I extend my rental period?",
    answer: "Yes, you can extend your rental by contacting us before your scheduled return time. Extension rates may vary from your original booking rate."
  },
  {
    question: "Do you offer electric vehicle rentals?",
    answer: "Yes, we have a growing fleet of electric vehicles including Tesla, Hyundai Ioniq, and other models. Charging is included with your rental."
  },
  {
    question: "What happens if the car breaks down?",
    answer: "All our vehicles include 24/7 roadside assistance. Contact our support team and we'll arrange repair or replacement vehicle as quickly as possible."
  },
  {
    question: "How do I find the car rental location?",
    answer: "Our locations are at major airports and city centers. After booking, you'll receive detailed directions and a map to the pickup location."
  }
];

async function generateEmbedding(text: string, ai: any): Promise<number[]> {
  const response = await ai.run('@cf/baai/bge-m3', { text: [text] });
  return response.data[0];
}

async function getCarsFromSupabase(): Promise<Car[]> {
  const response = await fetch('https://plqzygsrozwylyelhzue.supabase.co/rest/v1/cars?select=*', {
    headers: {
      'apikey': 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBscXp5Z3Nyb3p3eWx5ZWxoenVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIyMDczNTAsImV4cCI6MjA4Nzc4MzM1MH0.5HBjueHX9VCzux-NuLHuOLFClKIqa8NSiwi-V9pDRQ0',
      'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBscXp5Z3Nyb3p3eWx5ZWxoenVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIyMDczNTAsImV4cCI6MjA4Nzc4MzM1MH0.5HBjueHX9VCzux-NuLHuOLFClKIqa8NSiwi-V9pDRQ0'
    }
  });
  return response.json();
}

async function main() {
  const { createWorkersAI } = await import('workers-ai-provider');
  
  // This will be injected by wrangler when running as wrangler script
  const ai = (globalThis as any).AI;
  const vectorizeCars = (globalThis as any).VECTORIZE;
  const vectorizeFaq = (globalThis as any).VECTORIZE_FAQ;

  if (!ai) {
    console.log("AI binding not available in this context. Running in standalone mode...");
    
    // Use fetch to call the /embed endpoint of the worker
    const workerUrl = 'https://ai-worker.dangduytoan13l.workers.dev';
    
    console.log('Fetching cars from Supabase...');
    const cars = await getCarsFromSupabase();
    console.log(`Found ${cars.length} cars in database`);
    
    // Process cars in batches
    const batchSize = 10;
    for (let i = 0; i < cars.length; i += batchSize) {
      const batch = cars.slice(i, i + batchSize);
      console.log(`Processing cars ${i + 1} to ${i + batch.length}...`);
      
      for (const car of batch) {
        try {
          // Generate embedding using worker endpoint
          const text = `${car.make} ${car.model} ${car.year} ${car.body_style || ''} ${car.fuel_type || ''} ${car.transmission || ''}`.trim();
          const embedResponse = await fetch(`${workerUrl}/embed`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text })
          });
          
          if (!embedResponse.ok) {
            console.error(`Failed to get embedding for car ${car.id}: ${car.name}`);
            continue;
          }
          
          const { embedding } = await embedResponse.json();
          
          // Insert into Vectorize using wrangler
          const vectors = [{
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
          }];
          
          console.log(`Would insert car vector: ${car.id} - ${car.name}`);
        } catch (e) {
          console.error(`Error processing car ${car.id}:`, e);
        }
      }
    }
    
    console.log('\n--- FAQ VECTORS ---');
    for (const faq of faqs) {
      console.log(`Q: ${faq.question}`);
    }
    
    return;
  }

  console.log('Starting vector population...');
  
  // Get all cars
  console.log('Fetching cars from Supabase...');
  const cars = await getCarsFromSupabase();
  console.log(`Found ${cars.length} cars`);
  
  // Generate and insert car vectors
  console.log('Generating car embeddings and inserting into Vectorize...');
  const carVectors = [];
  
  for (const car of cars) {
    const text = `${car.make} ${car.model} ${car.year} ${car.body_style || ''} ${car.fuel_type || ''} ${car.transmission || ''} ${car.engine || ''}`.trim();
    const embedding = await generateEmbedding(text, ai);
    
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
  
  // Insert in batches of 1000
  const insertBatchSize = 1000;
  for (let i = 0; i < carVectors.length; i += insertBatchSize) {
    const batch = carVectors.slice(i, i + insertBatchSize);
    console.log(`Inserting car vectors ${i + 1} to ${i + batch.length}...`);
    await vectorizeCars.upsert(batch);
  }
  
  console.log(`Inserted ${carVectors.length} car vectors`);
  
  // Generate and insert FAQ vectors
  console.log('Generating FAQ embeddings and inserting into Vectorize...');
  const faqVectors = [];
  
  for (const faq of faqs) {
    const text = `${faq.question} ${faq.answer}`.trim();
    const embedding = await generateEmbedding(text, ai);
    
    faqVectors.push({
      id: String(faqVectors.length + 1),
      values: embedding,
      metadata: {
        question: faq.question,
        answer: faq.answer
      }
    });
  }
  
  await vectorizeFaq.upsert(faqVectors);
  console.log(`Inserted ${faqVectors.length} FAQ vectors`);
  
  console.log('Done!');
}

main().catch(console.error);
