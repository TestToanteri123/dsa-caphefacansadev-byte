export interface ProcessedAction {
  type: string;
  query?: string;
  payload?: any;
}

export interface SearchResult {
  id: string;
  name: string;
  price: number;
  brand: string;
  category: string;
  index: number;
}

export interface RentalInfoResult {
  carId?: number;
  carName?: string;
  days?: number;
  totalPrice?: number;
}

export function processCarIntent(toolResults: any[], userMessage: string): { action: ProcessedAction | null, searchResults: SearchResult[], rentalInfo: RentalInfoResult | null } {
  let action: ProcessedAction | null = null;
  let searchResults: SearchResult[] = [];
  let rentalInfo: RentalInfoResult | null = null;

  if (!toolResults || !Array.isArray(toolResults)) {
    return { action, searchResults, rentalInfo };
  }

  for (const tr of toolResults) {
    try {
      let parsed: any;
      if (tr.output?.content?.[0]?.text) {
        parsed = JSON.parse(tr.output.content[0].text);
      } else {
        parsed = typeof tr.result === 'string' ? JSON.parse(tr.result) : tr.result;
      }

      const toolName = tr.toolName;

      if (toolName === 'rentCar') {
        action = { type: 'rent_car', payload: parsed };
      } else if (toolName === 'searchCars') {
        action = { type: 'search', query: userMessage };
        if (parsed?.results) {
          searchResults = parsed.results.map((p: any, idx: number) => ({
            id: p.id,
            name: p.name,
            price: p.price,
            brand: p.brand || '',
            category: p.category || '',
            index: idx + 1,
          }));
        }
      } else if (toolName === 'filterCarsByPrice') {
        action = { type: 'filter', query: userMessage };
        if (parsed?.results) {
          searchResults = parsed.results.map((p: any, idx: number) => ({
            id: p.id,
            name: p.name,
            price: p.price,
            brand: p.brand || '',
            category: p.category || '',
            index: idx + 1,
          }));
        }
      } else if (toolName === 'viewRentals' && parsed?.success) {
        action = { type: 'view_rentals', payload: parsed };
      } else if (toolName === 'cancelRental' && parsed?.success) {
        action = { type: 'cancel_rental', payload: parsed };
      } else if (toolName === 'compareCars' && parsed?.cars?.length) {
        action = { type: 'compare', payload: parsed };
      } else if (toolName === 'getCarDetails' && parsed?.car) {
        action = { type: 'car_details', payload: parsed };
        const p = parsed.car;
        searchResults = [
          {
            id: p.id,
            name: p.name,
            price: p.price,
            brand: p.brand || '',
            category: p.category || '',
            index: 1,
          },
        ];
      } else if (toolName === 'getRentalStatus') {
        action = { type: 'rental_status', payload: parsed };
      } else if (toolName === 'rentalFlow') {
        const flowAction = parsed?.action || parsed?.step;
        if (flowAction === 'rental_confirmed' || flowAction === 'confirm') {
          action = { type: 'rent_car', payload: parsed };
          searchResults = [];
        } else if (flowAction === 'rental_cancelled' || flowAction === 'cancel') {
          action = { type: 'rental_cancelled', payload: parsed };
        } else if (parsed?.selectedCar) {
          action = { type: 'rental_flow', payload: parsed };
        }
      }
    } catch {
    }
  }

  let finalRentalInfo: RentalInfoResult | null = null;
  if (toolResults?.[0]?.output?.content?.[0]?.text) {
    try {
      const parsedText = JSON.parse(toolResults[0].output.content[0].text);
      if (parsedText?.rental) {
        finalRentalInfo = {
          carId: parsedText.rental.carId ? parseInt(parsedText.rental.carId) : undefined,
          carName: parsedText.rental.carName,
          days: parsedText.rental.days,
          totalPrice: parsedText.rental.totalPrice,
        };
      }
    } catch {}
  }

  return { action, searchResults, rentalInfo: finalRentalInfo };
}
