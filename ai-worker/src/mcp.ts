/// <reference types="@cloudflare/workers-types" />
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

export type Env = {
  AI: Fetcher;
  VECTORIZE_FAQ: VectorizeIndex;
  SUPABASE_URL: string;
  SUPABASE_ANON_KEY: string;
  SUPABASE_SERVICE_ROLE_KEY: string;
};

const STATIC_CARS = [
  {
    id: 1,
    name: 'Toyota Camry',
    make: 'Toyota',
    model: 'Camry',
    year: 2022,
    price: 280,
    dailyCost: 280,
    bodyStyle: 'Sedan',
    fuelType: 'Gasoline',
    transmission: 'Automatic',
    mileage: 25000,
    engine: '2.5L 4-cylinder',
    mpgCity: 28,
    mpgHighway: 39,
    drivetrain: 'FWD',
    exteriorColor: 'Silver',
    imageUrl: 'https://platform.cstatic-images.com/large/in/v2/68717a00-c5ec-5b49-b023-bbb0e783cffb/6c7dfd63-1d2f-4fa1-acbc-f1583a8fd191/lR-3g7EXhwhyoQYWP164mGpB0eo.jpg',
    availability: 5,
  },
  {
    id: 2,
    name: 'Honda Civic',
    make: 'Honda',
    model: 'Civic',
    year: 2023,
    price: 250,
    dailyCost: 250,
    bodyStyle: 'Sedan',
    fuelType: 'Gasoline',
    transmission: 'Automatic',
    mileage: 15000,
    engine: '2.0L 4-cylinder',
    mpgCity: 31,
    mpgHighway: 40,
    drivetrain: 'FWD',
    exteriorColor: 'White',
    imageUrl: 'https://platform.cstatic-images.com/large/in/v2/5395aee7-7fcb-5380-b589-d9a90c9902ad/2ebc2b21-d157-4bcb-a5a0-adc30905e089/GZlvKDymyM3zRvnaUJ0dkgwEN4k.jpg',
    availability: 3,
  },
  {
    id: 3,
    name: 'Ford Mustang',
    make: 'Ford',
    model: 'Mustang',
    year: 2021,
    price: 350,
    dailyCost: 350,
    bodyStyle: 'Coupe',
    fuelType: 'Gasoline',
    transmission: 'Manual',
    mileage: 45000,
    engine: '5.0L V8',
    mpgCity: 15,
    mpgHighway: 24,
    drivetrain: 'RWD',
    exteriorColor: 'Red',
    imageUrl: 'https://platform.cstatic-images.com/large/in/v2/a28e0302-a6fe-53cf-9ffc-375365516264/c3bce7f3-d40e-4c65-b8c3-6f480ffc69f0/6Z7VhGsEysv1MT5XW8qF8lt69l4.jpg',
    availability: 2,
  },
  {
    id: 4,
    name: 'Tesla Model 3',
    make: 'Tesla',
    model: 'Model 3',
    year: 2023,
    price: 120,
    dailyCost: 120,
    bodyStyle: 'Sedan',
    fuelType: 'Electric',
    transmission: 'Automatic',
    mileage: 8000,
    engine: 'Electric Motor',
    mpgCity: 138,
    mpgHighway: 126,
    drivetrain: 'RWD',
    exteriorColor: 'Black',
    imageUrl: 'https://platform.cstatic-images.com/large/in/v2/1d1be057-dc33-5f90-990b-c21610e43727/f24841c4-efa1-440a-af97-7a9fa1a7ebde/G0jaSi0PLL_w6_iYJ2DVbsqooLU.jpg',
    availability: 4,
  },
  {
    id: 5,
    name: 'Hyundai Tucson',
    make: 'Hyundai',
    model: 'Tucson',
    year: 2022,
    price: 300,
    dailyCost: 300,
    bodyStyle: 'SUV',
    fuelType: 'Gasoline',
    transmission: 'Automatic',
    mileage: 35000,
    engine: '2.5L 4-cylinder',
    mpgCity: 25,
    mpgHighway: 32,
    drivetrain: 'AWD',
    exteriorColor: 'Blue',
    imageUrl: 'https://platform.cstatic-images.com/large/in/v2/a3680048-8e7b-5a24-96d7-20888b791aa4/49a93a59-0164-4859-8e0b-30a41d52b073/VHxsPw5P-7AMYoZxTNUOzF77R-s.jpg',
    availability: 6,
  },
];

type LogLevel = 'info' | 'warn' | 'error';
function log(level: LogLevel, event: string, fields: Record<string, unknown> = {}): void {
  const entry = { ts: new Date().toISOString(), level, service: 'ai-worker-mcp', event, ...fields };
  level === 'error' ? console.error(JSON.stringify(entry)) : console.log(JSON.stringify(entry));
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

type LastSearchResult = { id: string; name: string; price: number; index: number };

function resolveOrdinal(text: string): number | null {
  const t = text.toLowerCase().trim();
  const wordMap: Record<string, number> = {
    'nhất': 1, 'một': 1, '1': 1, 'đầu tiên': 1,
    'hai': 2, '2': 2,
    'ba': 3, '3': 3,
    'tư': 4, 'bốn': 4, '4': 4,
    'năm': 5, '5': 5,
  };
  const m = t.match(/(?:thứ\s+|số\s+)(\S+)/);
  if (m) return wordMap[m[1]] ?? null;
  if (/đầu tiên/.test(t)) return 1;
  return null;
}

async function supabaseFetch(env: Env, endpoint: string, options: RequestInit = {}) {
  const url = `${env.SUPABASE_URL}/rest/v1/${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'apikey': env.SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${env.SUPABASE_ANON_KEY}`,
    ...options.headers,
  };
  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Supabase error: ${response.status} - ${error}`);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function supabaseFetchWithAuth(env: Env, endpoint: string, options: RequestInit = {}) {
  const url = `${env.SUPABASE_URL}/rest/v1/${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'apikey': env.SUPABASE_SERVICE_ROLE_KEY,
    'Authorization': `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
    'Prefer': 'return=representation',
    ...options.headers,
  };
  
  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Supabase error: ${response.status} - ${error}`);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

type StaticCar = typeof STATIC_CARS[number];

// Rental session state for conversational flow
type RentalSession = {
  step: 'idle' | 'awaiting_car' | 'awaiting_days' | 'awaiting_confirmation';
  selectedCar: { id: string; name: string; price: number } | null;
  rentalDays: number | null;
  startDate: string | null;
  endDate: string | null;
  totalPrice: number | null;
};

// In-memory rental sessions (per user)
const rentalSessions = new Map<string, RentalSession>();

function getRentalSession(userId: string): RentalSession {
  if (!rentalSessions.has(userId)) {
    rentalSessions.set(userId, {
      step: 'idle',
      selectedCar: null,
      rentalDays: null,
      startDate: null,
      endDate: null,
      totalPrice: null,
    });
  }
  return rentalSessions.get(userId)!;
}

function resetRentalSession(userId: string) {
  rentalSessions.set(userId, {
    step: 'idle',
    selectedCar: null,
    rentalDays: null,
    startDate: null,
    endDate: null,
    totalPrice: null,
  });
}

// Tool to manage conversational rental flow
function createRentalFlowTool() {
  return {
    name: 'rentalFlow',
    description: 'Manage the conversational car rental flow. Use this to track rental progress and confirm bookings.',
    inputSchema: {
      type: 'object',
      properties: {
        action: {
          type: 'string',
          enum: ['start', 'select_car', 'set_days', 'confirm', 'cancel', 'status'],
          description: 'The action to take in the rental flow'
        },
        carName: { type: 'string', description: 'Car name or ID' },
        carId: { type: 'string', description: 'Car ID' },
        days: { type: 'number', description: 'Number of rental days (1-7)' },
        userId: { type: 'string', description: 'User ID' },
      },
      required: ['action'],
    },
  };
}

function carToResult(car: StaticCar) {
  return {
    id: String(car.id),
    name: car.name,
    make: car.make,
    price: car.dailyCost,
    category: car.bodyStyle,
    image_url: car.imageUrl,
  };
}

function resolveCarFromStatic(
  carId: string | undefined,
  carName: string | undefined,
  lastSearchResults: LastSearchResult[] = [],
): ReturnType<typeof carToResult> | null {
  const textToCheck = carName || '';
  if (textToCheck && lastSearchResults.length > 0) {
    const ordinal = resolveOrdinal(textToCheck);
    if (ordinal !== null) {
      const match = lastSearchResults.find(p => p.index === ordinal) || lastSearchResults[ordinal - 1];
      if (match) {
        log('info', 'resolveCar.ordinal', { ordinal, resolvedId: match.id, resolvedName: match.name });
        const found = STATIC_CARS.find(c => String(c.id) === String(match.id));
        if (found) return carToResult(found);
        return { id: match.id, name: match.name, make: '', price: match.price, category: '', image_url: '' };
      }
    }
  }

  const cleanId = carId && carId !== 'null' ? carId.trim() : '';
  if (cleanId) {
    const found = STATIC_CARS.find(c => String(c.id) === cleanId);
    if (found) return carToResult(found);
  }

  const searchTerm = (carName || cleanId || '').toLowerCase();
  if (!searchTerm) return null;

  const found = STATIC_CARS.find(c =>
    c.name.toLowerCase().includes(searchTerm) ||
    c.make.toLowerCase().includes(searchTerm) ||
    c.model.toLowerCase().includes(searchTerm) ||
    searchTerm.includes(c.make.toLowerCase()) ||
    searchTerm.includes(c.model.toLowerCase())
  );
  return found ? carToResult(found) : null;
}

export function createRentCarMcpServer(env: Env, lastSearchResults: LastSearchResult[] = [], userId?: string) {
  const ctxUserId = userId;
  const server = new McpServer({
    name: 'Rent Car VoiceCommerce MCP Server',
    version: '1.0.0',
  });

  server.registerTool(
    'searchCars',
    {
      description: 'Search for rental cars by name, brand, or type.',
      inputSchema: z.object({
        query: z.string().optional().default('').describe('Car name, brand, or type in English'),
      })
    },
    async ({ query }) => {
      log('info', 'tool.searchCars', { query });
      try {
        const q = (query || '').toLowerCase().trim();
        const isAllQuery = q === '' || q === 'all' || q === 'any' || q === 'available' || q === 'cars';
        const results = isAllQuery
          ? STATIC_CARS
          : STATIC_CARS.filter(c =>
              c.name.toLowerCase().includes(q) ||
              c.make.toLowerCase().includes(q) ||
              c.model.toLowerCase().includes(q) ||
              c.bodyStyle.toLowerCase().includes(q) ||
              c.fuelType.toLowerCase().includes(q)
            );

        if (results.length === 0) {
          return { content: [{ type: 'text', text: JSON.stringify({ results: [], message: `No cars found for "${query}"` }) }] };
        }

        const mappedResults = results.map((r, i) => ({
          id: String(r.id),
          name: r.name,
          make: r.make,
          price: r.dailyCost,
          category: r.bodyStyle,
          index: i + 1,
        }));

        const listText = mappedResults
          .map((r, idx) => `${idx + 1}. ${r.name} (${r.make}) - $${r.price}/day`)
          .join('; ');
        const message = `Found ${mappedResults.length} cars: ${listText}. Which one would you like to choose?`;

        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              results: mappedResults,
              count: mappedResults.length,
              message,
            })
          }]
        };
      } catch (e) {
        log('error', 'searchCars error', { error: String(e) });
        return { content: [{ type: 'text', text: JSON.stringify({ results: [], message: 'Search error. Please try again.' }) }] };
      }
    }
  );

  server.registerTool(
    'filterCarsByPrice',
    {
      description: 'Search for cars within a price range.',
      inputSchema: z.object({
        query: z.string().describe('Car type or name'),
        minPrice: z.union([z.number(), z.string()]).optional().describe('Minimum price per day in USD'),
        maxPrice: z.union([z.number(), z.string()]).optional().describe('Maximum price per day in USD'),
      })
    },
    async ({ query, minPrice, maxPrice }) => {
      log('info', 'tool.filterCarsByPrice', { query, minPrice, maxPrice });
      try {
        const minP = typeof minPrice === 'string' ? parseInt(minPrice) : (minPrice || 0);
        const maxP = typeof maxPrice === 'string' ? parseInt(maxPrice) : (maxPrice || 100000);
        const q = query.toLowerCase();

        const results = STATIC_CARS.filter(c => {
          const inRange = c.dailyCost >= minP && c.dailyCost <= maxP;
          const matchesQuery = !q || c.name.toLowerCase().includes(q) || c.make.toLowerCase().includes(q) || c.bodyStyle.toLowerCase().includes(q);
          return inRange && matchesQuery;
        });

        const mappedResults = results.slice(0, 5).map(r => ({
          id: String(r.id),
          name: r.name,
          make: r.make,
          price: r.dailyCost,
          category: r.bodyStyle,
        }));

        let message = `No ${query} cars found in this price range.`;
        if (mappedResults.length > 0) {
          const overview = mappedResults.slice(0, 3).map(r => `${r.name} at $${r.price}/day`).join(', ');
          message = `Found ${mappedResults.length} ${query} cars in this price range. Options: ${overview}.`;
        }

        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              results: mappedResults,
              count: mappedResults.length,
              priceRange: { min: minPrice, max: maxPrice },
              message,
            })
          }]
        };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ results: [], message: 'Error filtering cars.' }) }] };
      }
    }
  );

  server.registerTool(
    'getCarDetails',
    {
      description: 'Get detailed information about a specific car by ID or name, including specifications.',
      inputSchema: z.object({
        carId: z.string().optional().describe('Car ID from the previously shown list'),
        carName: z.string().optional().describe('Car name or reference like "car number 2" or "Toyota Camry"'),
      })
    },
    async ({ carId, carName }) => {
      log('info', 'tool.getCarDetails', { carId, carName });
      try {
        const car = resolveCarFromStatic(carId, carName, lastSearchResults);

        if (!car) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Car not found.' }) }] };
        }

        const staticCar = STATIC_CARS.find(c => String(c.id) === car.id);
        const specList = staticCar ? [
          { label: 'Make', value: staticCar.make },
          { label: 'Model', value: staticCar.model },
          { label: 'Year', value: staticCar.year },
          { label: 'Fuel', value: staticCar.fuelType },
          { label: 'Transmission', value: staticCar.transmission },
          { label: 'Mileage', value: `${staticCar.mileage} km` },
          { label: 'MPG City', value: staticCar.mpgCity },
          { label: 'MPG Highway', value: staticCar.mpgHighway },
          { label: 'Engine', value: staticCar.engine },
          { label: 'Drivetrain', value: staticCar.drivetrain },
          { label: 'Color', value: staticCar.exteriorColor },
        ].filter(s => s.value) : [];

        const responsePayload: any = {
          success: true,
          car: {
            id: car.id,
            name: car.name,
            make: car.make,
            price: car.price,
            category: car.category,
            priceFormatted: `$${car.price}/day`,
            image_url: car.image_url,
            specs: specList,
          },
          action: 'car_details',
          message: specList.length > 0
            ? `${car.name} - $${car.price}/day. Specs: ${specList.slice(0, 4).map(s => `${s.label}: ${s.value}`).join(', ')}... Would you like to rent this car or need more information?`
            : `${car.name} - $${car.price}/day. Would you like to rent this car or need more information?`,
        };

        return {
          content: [{ type: 'text', text: JSON.stringify(responsePayload) }]
        };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Error getting car details.' }) }] };
      }
    }
  );

  server.registerTool(
    'viewRentals',
    {
      description: 'View rental history and current rentals for a user.',
      inputSchema: z.object({
        userId: z.string().describe('User ID to get rental list'),
      })
    },
    async ({ userId }) => {
      const isValidId = userId && /^[a-zA-Z0-9_-]+$/.test(userId) && userId.length > 5;
      const uid = isValidId ? userId : ctxUserId;
      log('info', 'tool.viewRentals', { userId: uid });
      try {
        if (!env.SUPABASE_URL) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Unable to access rental data at this time.' }) }] };
        }

        const rentals = await supabaseFetch(env, `rentals?user_id=eq.${uid}&order=created_at.desc&limit=10`);

        if (!rentals || rentals.length === 0) {
          return {
            content: [{ type: 'text', text: JSON.stringify({ success: true, rentals: [], action: 'view_rentals', message: 'You have no rental history yet.' }) }]
          };
        }

        const summary = rentals.map((r: any, i: number) => {
          const statusText = r.status === 'active' ? 'Active' : r.status === 'completed' ? 'Completed' : r.status === 'cancelled' ? 'Cancelled' : r.status;
          return `${i + 1}. Car ID ${r.car_id} - ${statusText}`;
        }).join(', ');

        return {
          content: [{
            type: 'text',
            text: JSON.stringify({ 
              success: true, 
              rentals, 
              action: 'view_rentals', 
              message: `You have ${rentals.length} rentals: ${summary}.`
            })
          }]
        };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Unable to view rental history. Please try again.' }) }] };
      }
    }
  );

  server.registerTool(
    'compareCars',
    {
      description: 'Compare 2 or more cars side by side with full specifications. After getting results, provide detailed comparison of key specs and use case recommendations.',
      inputSchema: z.object({
        cars: z.union([
          z.array(z.object({
            carId: z.string().optional(),
            carName: z.string().optional(),
          })),
          z.string().transform(s =>
            s.split(/[,;]+/).map(p => p.trim()).filter(Boolean).map(p => ({ carName: p }))
          ),
        ]).describe('Cars to compare - array of {carId?,carName?} or comma-separated list'),
      })
    },
    async ({ cars }) => {
      try {
        const refs = Array.isArray(cars) ? cars : (cars as any[]);
        const comparisons: any[] = [];
        for (const ref of refs.slice(0, 3)) {
          const resolved = resolveCarFromStatic(
            (ref as any).carId,
            (ref as any).carName,
            lastSearchResults,
          );
          if (resolved) {
            const staticCar = STATIC_CARS.find(c => String(c.id) === resolved.id);
            comparisons.push({
              id: resolved.id,
              name: resolved.name,
              make: resolved.make,
              price: resolved.price,
              priceFormatted: `$${resolved.price}/day`,
              category: resolved.category,
              specs: staticCar ? {
                make: staticCar.make,
                model: staticCar.model,
                year: staticCar.year,
                fuel_type: staticCar.fuelType,
                transmission: staticCar.transmission,
                engine: staticCar.engine,
                mpg_city: staticCar.mpgCity,
                mpg_highway: staticCar.mpgHighway,
                drivetrain: staticCar.drivetrain,
              } : null,
            });
          }
        }
        let message = 'Not enough cars to compare.';
        if (comparisons.length >= 2) {
          const prompt = comparisons.map(p => {
            const specText = p.specs ? 
              `Make: ${p.specs.make}, Model: ${p.specs.model}, Year: ${p.specs.year}, Fuel: ${p.specs.fuel_type}, Transmission: ${p.specs.transmission}, Engine: ${p.specs.engine}, MPG: ${p.specs.mpg_city}/${p.specs.mpg_highway}` 
              : 'No specs available';
            return `${p.name} (${p.priceFormatted}):\n${specText}`;
          }).join('\n\n---\n\n');

          try {
            const glmResult = await (env.AI as any).run('@cf/zai-org/glm-4.7-flash', {
              messages: [
                { role: 'system', content: 'You are a car rental consultant at Rent A Car. Compare the cars and suggest the best use case for each vehicle. Keep responses brief (max 4 sentences). Use plain prose, NO tables, NO markdown, NO bullet points.' },
                { role: 'user', content: prompt }
              ]
            });
            message = glmResult?.choices?.[0]?.message?.content || glmResult?.response || comparisons.map(p => p.name).join(' vs ');
          } catch {
            message = comparisons.map(p => `${p.name} (${p.priceFormatted})`).join(' vs ');
          }
        }
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              cars: comparisons,
              count: comparisons.length,
              action: 'compare',
              message,
            })
          }]
        };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ cars: [], message: 'Error comparing cars.' }) }] };
      }
    }
  );

  server.registerTool(
    'rentCar',
    {
      description: 'Rent a specific car. Requires carId or carName, start date, and end date.',
      inputSchema: z.object({
        carName: z.string().optional().describe('Car name if specified by user'),
        carId: z.string().optional().describe('Car ID to rent'),
        startDate: z.string().describe('Rental start date (YYYY-MM-DD)'),
        endDate: z.string().describe('Rental end date (YYYY-MM-DD)'),
        userId: z.string().optional().describe('User ID'),
      })
    },
    async ({ carName, carId, startDate, endDate, userId }) => {
      const uid = userId || ctxUserId;
      log('info', 'tool.rentCar', { carName, carId, startDate, endDate, userId: uid });
      try {
        const car = resolveCarFromStatic(carId, carName, lastSearchResults);

        if (!car || !car.id) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Car not found. Please provide a clearer car name.' }) }] };
        }

        if (!uid) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Please log in to rent a car.' }) }] };
        }

        if (!startDate || !endDate) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Please provide start and end dates for your rental.' }) }] };
        }

        const start = new Date(startDate);
        const end = new Date(endDate);
        const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
        
        if (days < 1) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'End date must be after start date.' }) }] };
        }

        const totalPrice = car.price * days;

        // Local rental only - no Supabase persistence

        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              success: true,
              car,
              rental: {
                startDate,
                endDate,
                days,
                totalPrice,
                totalPriceFormatted: `$${totalPrice}`,
              },
              action: 'rent_car',
              message: `Rental created for ${car.name} from ${startDate} to ${endDate} (${days} days). Total: $${totalPrice}.`,
            })
          }]
        };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Unable to rent car. Please try again.' }) }] };
      }
    }
  );

  server.registerTool(
    'cancelRental',
    {
      description: 'Cancel a pending or confirmed rental reservation.',
      inputSchema: z.object({
        rentalId: z.string().describe('Rental ID or order number to cancel'),
        userId: z.string().optional().describe('User ID to verify ownership'),
      })
    },
    async ({ rentalId, userId }) => {
      log('info', 'tool.cancelRental', { rentalId, userId });
      try {
        if (!env.SUPABASE_URL) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Unable to cancel rental at this time.' }) }] };
        }

        const uid = userId || ctxUserId;
        
        const rentals = await supabaseFetch(env, `rentals?id=eq.${rentalId}`);
        
        if (!rentals || rentals.length === 0) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: `Rental "${rentalId}" not found.` }) }] };
        }

        const rental = rentals[0];

        if (uid && rental.user_id !== uid) {
          return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'You do not have permission to cancel this rental.' }) }] };
        }

        const nonCancellableStatuses = ['completed', 'cancelled'];
        if (nonCancellableStatuses.includes(rental.status)) {
          const statusMap: Record<string, string> = { completed: 'already completed', cancelled: 'already cancelled' };
          return {
            content: [{ type: 'text', text: JSON.stringify({ success: false, message: `Rental #${rentalId.slice(0, 8).toUpperCase()} cannot be cancelled because it has ${statusMap[rental.status]}.` }) }]
          };
        }

        await supabaseFetchWithAuth(env, `rentals?id=eq.${rentalId}`, {
          method: 'PATCH',
          body: JSON.stringify({ 
            status: 'cancelled'
          }),
        });

        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              success: true,
              rentalId: rental.id,
              action: 'cancel_rental',
              message: `Rental for Car ID ${rental.car_id} has been successfully cancelled.`,
            })
          }]
        };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Unable to cancel rental. Please try again.' }) }] };
      }
    }
  );

  server.registerTool(
    'getRentalStatus',
    {
      description: 'Check the status of a rental reservation.',
      inputSchema: z.object({
        rentalId: z.string().optional().describe('Rental order number'),
        userId: z.string().optional().describe('User ID to list all rentals'),
      })
    },
    async ({ rentalId, userId }) => {
      const isValidId = userId && /^[a-zA-Z0-9_-]+$/.test(userId) && userId.length > 5;
      const uid = isValidId ? userId : ctxUserId;
      log('info', 'tool.getRentalStatus', { rentalId, userId: uid });
      try {
        if (!env.SUPABASE_URL) return { content: [{ type: 'text', text: JSON.stringify({ rentals: [], message: 'Lookup error.' }) }] };

        if (rentalId) {
          const rentals = await supabaseFetch(env, `rentals?id=eq.${rentalId}`);
          if (!rentals || rentals.length === 0) return { content: [{ type: 'text', text: JSON.stringify({ rentals: [], message: `Rental "${rentalId}" not found.` }) }] };

          const rental = rentals[0];
          const statusText = rental.status === 'active' ? 'Active' : rental.status === 'completed' ? 'Completed' : rental.status === 'cancelled' ? 'Cancelled' : rental.status;
          return {
            content: [{
              type: 'text',
              text: JSON.stringify({
                rentals,
                rental: { ...rental, statusText },
                action: 'rental_status',
                message: `Rental #${rental.id}: ${statusText}`,
              })
            }]
          };
        }

        if (uid) {
          const rentals = await supabaseFetch(env, `rentals?user_id=eq.${uid}&order=created_at.desc&limit=5`);
          return {
             content: [{
               type: 'text', text: JSON.stringify({
                 rentals, action: 'rental_status',
                 message: rentals && rentals.length ? `You have ${rentals.length} recent rentals.` : 'You have no rentals yet.'
               })
             }]
          };
        }
        return { content: [{ type: 'text', text: JSON.stringify({ rentals: [], action: 'rental_status', message: 'Please provide a rental order number.' }) }] };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ rentals: [], message: 'System error.' }) }] };
      }
    }
  );

  server.registerTool(
    'getRentalFaq',
    {
      description: 'Answer frequently asked questions about car rental policies.',
      inputSchema: z.object({ question: z.string().describe('FAQ question') })
    },
    async ({ question }) => {
      try {
        if (!env.VECTORIZE_FAQ) {
          const faqAnswers: Record<string, string> = {
            'rent': 'You can rent a car by searching for your desired vehicle and selecting rent. You need to provide start and end dates.',
            'cancel': 'You can cancel a rental if the status is still active. Contact hotline for assistance.',
            'documents': 'When picking up the car, you need to bring your drivers license and ID/passport.',
            'deposit': 'We require a deposit when renting. The deposit will be refunded when you return the car.',
            'insurance': 'All rentals include basic insurance. You can purchase additional comprehensive coverage.',
            'return': 'You need to return the car at the agreed time. Late returns will incur additional fees.',
          };
          
          const lowerQ = question.toLowerCase();
          for (const [key, answer] of Object.entries(faqAnswers)) {
            if (lowerQ.includes(key)) {
              return { content: [{ type: 'text', text: JSON.stringify({ answer, action: 'faq' }) }] };
            }
          }
          return { content: [{ type: 'text', text: JSON.stringify({ answer: 'Please contact hotline for detailed assistance.', action: 'faq' }) }] };
        }
        
        const embedding = await generateEmbedding(question, env);
        if (!embedding.length) return { content: [{ type: 'text', text: JSON.stringify({ answer: 'Error.' }) }] };

        const result = await env.VECTORIZE_FAQ.query(embedding, { topK: 1, returnMetadata: 'all' });
        const best = result?.matches?.[0];
        if (best?.score && best.score >= 0.45 && best.metadata?.answer) {
          return { content: [{ type: 'text', text: JSON.stringify({ answer: best.metadata.answer, action: 'faq' }) }] };
        }
        return { content: [{ type: 'text', text: JSON.stringify({ answer: 'Could not find an answer, please contact hotline.', action: 'faq' }) }] };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ answer: 'FAQ error.' }) }] };
      }
    }
  );

  server.registerTool(
    'createSupportTicket',
    {
      description: 'Create a customer support ticket for car rental issues.',
      inputSchema: z.object({
        category: z.enum(['rental_issue', 'delivery', 'payment', 'return', 'insurance', 'other']).describe('Support category'),
        message: z.string().describe('Issue description'),
        userId: z.string().optional().describe('User ID'),
      })
    },
    async ({ category, message, userId }) => {
      try {
        const ticketId = crypto.randomUUID();
        
        const supportMessage = `Support Request - Category: ${category}\nMessage: ${message}\nUser ID: ${userId || 'Guest'}`;
        
        log('info', 'tool.createSupportTicket', { ticketId, category, userId });
        
        return {
           content: [{
             type: 'text',
             text: JSON.stringify({
               success: true, ticketId, category, action: 'create_ticket',
               message: `Support ticket #${ticketId.slice(0, 8).toUpperCase()} has been created. We will contact you soon.`,
               note: supportMessage
             })
           }]
         };
      } catch (e) {
        return { content: [{ type: 'text', text: JSON.stringify({ success: false, message: 'Error creating ticket.' }) }] };
      }
    }
  );

  server.registerTool(
    'rentalFlow',
    {
      description: 'Manage the conversational car rental flow. Use this to guide users through: 1) select car, 2) set days, 3) confirm rental.',
      inputSchema: z.object({
        action: z.enum(['start', 'select_car', 'set_days', 'confirm', 'cancel', 'status']).describe('The action to take in the rental flow'),
        carName: z.string().optional().describe('Car name or ID'),
        carId: z.string().optional().describe('Car ID from search results'),
        days: z.number().optional().describe('Number of rental days (1-7)'),
        userId: z.string().optional().describe('User ID'),
      })
    },
    async ({ action, carName, carId, days, userId }) => {
      const uid = userId || ctxUserId || 'guest';
      const session = getRentalSession(uid);

      log('info', 'tool.rentalFlow', { action, carName, carId, days, userId: uid, currentStep: session.step });

      try {
        switch (action) {
          case 'start': {
            resetRentalSession(uid);
            session.step = 'awaiting_car';
            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  success: true,
                  step: 'awaiting_car',
                  message: 'Sure, I can help you rent a car. Which car would you like to rent? We have Toyota Camry, Honda Civic, Ford Mustang, Tesla Model 3, and Hyundai Tucson available.',
                  cars: STATIC_CARS.map(c => ({ id: String(c.id), name: c.name, price: c.dailyCost })),
                  action: 'rental_start'
                })
              }]
            };
          }

          case 'select_car': {
            const car = resolveCarFromStatic(carId, carName, lastSearchResults);
            if (!car) {
              return {
                content: [{
                  type: 'text',
                  text: JSON.stringify({
                    success: false,
                    step: session.step,
                    message: 'I could not find that car. Please provide a car name like Toyota Camry, Honda Civic, Ford Mustang, Tesla Model 3, or Hyundai Tucson.'
                  })
                }]
              };
            }

            session.selectedCar = { id: car.id, name: car.name, price: car.price };
            session.step = 'awaiting_days';

            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  success: true,
                  step: 'awaiting_days',
                  selectedCar: session.selectedCar,
                  message: `Great! You've selected ${car.name} at $${car.price}/day. How many days would you like to rent it? (Maximum 7 days)`,
                  action: 'rental_car_selected'
                })
              }]
            };
          }

          case 'set_days': {
            if (!session.selectedCar) {
              return {
                content: [{
                  type: 'text',
                  text: JSON.stringify({
                    success: false,
                    step: session.step,
                    message: 'Please select a car first.'
                  })
                }]
              };
            }

            const rentalDays = days || 1;
            if (rentalDays < 1 || rentalDays > 7) {
              return {
                content: [{
                  type: 'text',
                  text: JSON.stringify({
                    success: false,
                    step: session.step,
                    message: 'Please choose between 1 and 7 days.'
                  })
                }]
              };
            }

            session.rentalDays = rentalDays;
            session.totalPrice = session.selectedCar.price * rentalDays;

            const today = new Date();
            const endDate = new Date(today);
            endDate.setDate(today.getDate() + rentalDays);
            session.startDate = today.toISOString().split('T')[0];
            session.endDate = endDate.toISOString().split('T')[0];

            session.step = 'awaiting_confirmation';

            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  success: true,
                  step: 'awaiting_confirmation',
                  selectedCar: session.selectedCar,
                  rentalDays: session.rentalDays,
                  startDate: session.startDate,
                  endDate: session.endDate,
                  totalPrice: session.totalPrice,
                  message: `Perfect! You're renting ${session.selectedCar.name} for ${rentalDays} days. Pickup: ${session.startDate}, Return: ${session.endDate}. Total cost: $${session.totalPrice} credits. Should I confirm this rental? Please say "yes" to confirm or "no" to cancel.`,
                  action: 'rental_confirmation'
                })
              }]
            };
          }

          case 'confirm': {
            if (!session.selectedCar || !session.rentalDays || !session.totalPrice) {
              return {
                content: [{
                  type: 'text',
                  text: JSON.stringify({
                    success: false,
                    step: session.step,
                    message: 'No rental in progress. Say "I want to rent a car" to start.'
                  })
                }]
              };
            }

            // Local rental only - no Supabase persistence
            const confirmation = {
              success: true,
              rental: {
                carId: session.selectedCar.id,
                carName: session.selectedCar.name,
                startDate: session.startDate,
                endDate: session.endDate,
                days: session.rentalDays,
                totalPrice: session.totalPrice,
                totalPriceFormatted: `$${session.totalPrice} credits`,
              },
              message: `Your rental is confirmed! You've booked ${session.selectedCar.name} from ${session.startDate} to ${session.endDate} (${session.rentalDays} days). Total: $${session.totalPrice} credits. Thank you for choosing Rent A Car!`,
              action: 'rental_confirmed'
            };

            resetRentalSession(uid);

            return {
              content: [{
                type: 'text',
                text: JSON.stringify(confirmation)
              }]
            };
          }

          case 'cancel': {
            const cancelledCar = session.selectedCar?.name || 'rental';
            resetRentalSession(uid);
            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  success: true,
                  step: 'idle',
                  message: `Your ${cancelledCar} rental has been cancelled. Is there anything else I can help you with?`,
                  action: 'rental_cancelled'
                })
              }]
            };
          }

          case 'status': {
            if (session.step === 'idle') {
              return {
                content: [{
                  type: 'text',
                  text: JSON.stringify({
                    step: 'idle',
                    message: 'No rental in progress. Say "I want to rent a car" to start.',
                  })
                }]
              };
            }

            return {
              content: [{
                type: 'text',
                text: JSON.stringify({
                  step: session.step,
                  selectedCar: session.selectedCar,
                  rentalDays: session.rentalDays,
                  totalPrice: session.totalPrice,
                  message: session.step === 'awaiting_car' ? 'Please select a car.' :
                    session.step === 'awaiting_days' ? `You've selected ${session.selectedCar?.name}. How many days?` :
                    `Rental: ${session.selectedCar?.name} for ${session.rentalDays} days ($${session.totalPrice}). Say "yes" to confirm or "no" to cancel.`,
                })
              }]
            };
          }

          default:
            return {
              content: [{
                type: 'text',
                text: JSON.stringify({ success: false, message: 'Unknown action.' })
              }]
            };
        }
      } catch (e) {
        log('error', 'rentalFlow error', { error: String(e) });
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({ success: false, message: 'Error in rental flow. Please try again.' })
          }]
        };
      }
    }
  );

  return server;
}
