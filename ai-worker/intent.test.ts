import { describe, expect, test } from 'bun:test';
import { processCarIntent } from './src/intent';

describe('processCarIntent', () => {
  describe('edge cases', () => {
    test('should return empty action and searchResults for null toolResults', () => {
      const result = processCarIntent(null as any, 'test message');
      expect(result.action).toBeNull();
      expect(result.searchResults).toEqual([]);
    });

    test('should return empty action and searchResults for undefined toolResults', () => {
      const result = processCarIntent(undefined as any, 'test message');
      expect(result.action).toBeNull();
      expect(result.searchResults).toEqual([]);
    });

    test('should return empty action and searchResults for empty array', () => {
      const result = processCarIntent([], 'test message');
      expect(result.action).toBeNull();
      expect(result.searchResults).toEqual([]);
    });

    test('should return empty action and searchResults for non-array toolResults', () => {
      const result = processCarIntent('not an array' as any, 'test message');
      expect(result.action).toBeNull();
      expect(result.searchResults).toEqual([]);
    });
  });

  describe('rentCar tool', () => {
    test('should process rentCar tool result', () => {
      const toolResults = [
        {
          toolName: 'rentCar',
          output: {
            content: [{
              text: JSON.stringify({
                success: true,
                car: { id: '21', name: 'Hyundai PALISADE' },
                rental: { startDate: '2026-03-01', endDate: '2026-03-05', days: 5, totalPrice: 10500 }
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Thuê xe Hyundai');
      
      expect(result.action).not.toBeNull();
      expect(result.action?.type).toBe('rent_car');
      expect(result.action?.payload).toBeDefined();
      expect(result.action?.payload.success).toBe(true);
      expect(result.action?.payload.car.name).toBe('Hyundai PALISADE');
    });

    test('should handle rentCar failure', () => {
      const toolResults = [
        {
          toolName: 'rentCar',
          output: {
            content: [{
              text: JSON.stringify({ success: false, message: 'Xe không tìm thấy' })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Thuê xe');
      
      expect(result.action).not.toBeNull();
      expect(result.action?.type).toBe('rent_car');
      expect(result.action?.payload.success).toBe(false);
    });
  });

  describe('searchCars tool', () => {
    test('should process searchCars with results', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: JSON.stringify({
                results: [
                  { id: '21', name: 'Hyundai PALISADE', price: 2100, make: 'Hyundai', body_style: 'SUV' },
                  { id: '22', name: 'Toyota Camry', price: 1500, make: 'Toyota', body_style: 'Sedan' }
                ],
                count: 2
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Tìm xe SUV');
      
      expect(result.action).not.toBeNull();
      expect(result.action?.type).toBe('search');
      expect(result.action?.query).toBe('Tìm xe SUV');
      expect(result.searchResults).toHaveLength(2);
      expect(result.searchResults[0].id).toBe('21');
      expect(result.searchResults[0].name).toBe('Hyundai PALISADE');
      expect(result.searchResults[0].price).toBe(2100);
      expect(result.searchResults[0].index).toBe(1);
      expect(result.searchResults[1].index).toBe(2);
    });

    test('should process searchCars with empty results', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: JSON.stringify({ results: [], count: 0, message: 'Không tìm thấy' })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Tìm xe abc');
      
      expect(result.action?.type).toBe('search');
      expect(result.searchResults).toEqual([]);
    });

    test('should handle missing results in searchCars', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: JSON.stringify({ message: 'No results' })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Tìm');
      
      expect(result.action?.type).toBe('search');
      expect(result.searchResults).toEqual([]);
    });
  });

  describe('filterCarsByPrice tool', () => {
    test('should process filterCarsByPrice with results', () => {
      const toolResults = [
        {
          toolName: 'filterCarsByPrice',
          output: {
            content: [{
              text: JSON.stringify({
                results: [
                  { id: '30', name: 'Toyota Camry', price: 1500, make: 'Toyota' },
                  { id: '31', name: 'Toyota Camry XLE', price: 2100, make: 'Toyota' }
                ],
                count: 2,
                priceRange: { min: 1000, max: 3000 }
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Xe dưới 3 triệu');
      
      expect(result.action?.type).toBe('filter');
      expect(result.action?.query).toBe('Xe dưới 3 triệu');
      expect(result.searchResults).toHaveLength(2);
      expect(result.searchResults[0].price).toBe(1500);
      expect(result.searchResults[1].price).toBe(2100);
    });
  });

  describe('viewRentals tool', () => {
    test('should process viewRentals with success', () => {
      const toolResults = [
        {
          toolName: 'viewRentals',
          output: {
            content: [{
              text: JSON.stringify({
                success: true,
                rentals: [
                  { id: 'r1', car_id: 21, status: 'active', start_date: '2026-03-01' },
                  { id: 'r2', car_id: 30, status: 'completed', start_date: '2026-02-15' }
                ]
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Xem lịch sử thuê');
      
      expect(result.action?.type).toBe('view_rentals');
      expect(result.action?.payload.success).toBe(true);
      expect(result.action?.payload.rentals).toHaveLength(2);
    });

    test('should not process viewRentals when success is false', () => {
      const toolResults = [
        {
          toolName: 'viewRentals',
          output: {
            content: [{
              text: JSON.stringify({ success: false, message: 'Lỗi' })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Xem lịch sử');
      
      expect(result.action).toBeNull();
    });
  });

  describe('cancelRental tool', () => {
    test('should process cancelRental with success', () => {
      const toolResults = [
        {
          toolName: 'cancelRental',
          output: {
            content: [{
              text: JSON.stringify({
                success: true,
                rentalId: 'r1',
                message: 'Đã hủy thành công'
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Hủy đơn thuê');
      
      expect(result.action?.type).toBe('cancel_rental');
      expect(result.action?.payload.success).toBe(true);
    });

    test('should not process cancelRental when success is false', () => {
      const toolResults = [
        {
          toolName: 'cancelRental',
          output: {
            content: [{
              text: JSON.stringify({ success: false, message: 'Không thể hủy' })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Hủy đơn');
      
      expect(result.action).toBeNull();
    });
  });

  describe('compareCars tool', () => {
    test('should process compareCars with cars', () => {
      const toolResults = [
        {
          toolName: 'compareCars',
          output: {
            content: [{
              text: JSON.stringify({
                cars: [
                  { id: '21', name: 'Hyundai PALISADE', price: 2100 },
                  { id: '30', name: 'Toyota Camry', price: 1500 }
                ],
                count: 2
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'So sánh Hyundai và Toyota');
      
      expect(result.action?.type).toBe('compare');
      expect(result.action?.payload.cars).toHaveLength(2);
    });

    test('should not process compareCars with empty cars', () => {
      const toolResults = [
        {
          toolName: 'compareCars',
          output: {
            content: [{
              text: JSON.stringify({ cars: [], count: 0 })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'So sánh');
      
      expect(result.action).toBeNull();
    });
  });

  describe('getCarDetails tool', () => {
    test('should process getCarDetails with car', () => {
      const toolResults = [
        {
          toolName: 'getCarDetails',
          output: {
            content: [{
              text: JSON.stringify({
                success: true,
                car: {
                  id: '21',
                  name: 'Hyundai PALISADE',
                  make: 'Hyundai',
                  price: 2100,
                  category: 'SUV'
                }
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Chi tiết xe Hyundai');
      
      expect(result.action?.type).toBe('car_details');
      expect(result.action?.payload.car.name).toBe('Hyundai PALISADE');
      expect(result.searchResults).toHaveLength(1);
      expect(result.searchResults[0].id).toBe('21');
      expect(result.searchResults[0].index).toBe(1);
    });

    test('should not process getCarDetails when car is missing', () => {
      const toolResults = [
        {
          toolName: 'getCarDetails',
          output: {
            content: [{
              text: JSON.stringify({ success: false })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Chi tiết');
      
      expect(result.action).toBeNull();
    });
  });

  describe('getRentalStatus tool', () => {
    test('should process getRentalStatus', () => {
      const toolResults = [
        {
          toolName: 'getRentalStatus',
          output: {
            content: [{
              text: JSON.stringify({
                rentals: [
                  { id: 'r1', status: 'active' }
                ]
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Kiểm tra trạng thái');
      
      expect(result.action?.type).toBe('rental_status');
      expect(result.action?.payload.rentals).toHaveLength(1);
    });
  });

  describe('unknown tool handling', () => {
    test('should return null action for unknown tools', () => {
      const toolResults = [
        {
          toolName: 'unknownTool',
          output: {
            content: [{
              text: JSON.stringify({ data: 'some data' })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Some message');
      
      expect(result.action).toBeNull();
      expect(result.searchResults).toEqual([]);
    });

    test('should skip malformed tool results gracefully', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: 'invalid json {'
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Test');
      
      expect(result.action).toBeNull();
    });

    test('should handle tool result without output property', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          result: JSON.stringify({ results: [{ id: '1', name: 'Test' }] })
        }
      ];

      const result = processCarIntent(toolResults, 'Test');
      
      expect(result.action?.type).toBe('search');
    });

    test('should handle tool result with string result', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          result: '{"results": [{"id": "1", "name": "Test Car"}]}'
        }
      ];

      const result = processCarIntent(toolResults, 'Test');
      
      expect(result.action?.type).toBe('search');
      expect(result.searchResults).toHaveLength(1);
    });
  });

  describe('multiple tool results', () => {
    test('should use the last valid tool result', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: JSON.stringify({ results: [{ id: '1', name: 'Car 1' }] })
            }]
          }
        },
        {
          toolName: 'getCarDetails',
          output: {
            content: [{
              text: JSON.stringify({ success: true, car: { id: '2', name: 'Car 2' } })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Test');
      
      expect(result.action?.type).toBe('car_details');
    });
  });

  describe('brand and category mapping', () => {
    test('should handle car with brand', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: JSON.stringify({
                results: [{ id: '21', name: 'Hyundai PALISADE', price: 2100, brand: 'Hyundai', category: 'SUV' }]
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Tìm');
      
      expect(result.searchResults[0].brand).toBe('Hyundai');
    });

    test('should handle car without brand (default to empty)', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: JSON.stringify({
                results: [{ id: '21', name: 'Some Car', price: 2100 }]
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Tìm');
      
      expect(result.searchResults[0].brand).toBe('');
    });

    test('should handle car without category (default to empty)', () => {
      const toolResults = [
        {
          toolName: 'searchCars',
          output: {
            content: [{
              text: JSON.stringify({
                results: [{ id: '21', name: 'Some Car', price: 2100 }]
              })
            }]
          }
        }
      ];

      const result = processCarIntent(toolResults, 'Tìm');
      
      expect(result.searchResults[0].category).toBe('');
    });
  });
});
