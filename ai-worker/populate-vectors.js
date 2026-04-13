const CLOUDFLARE_ACCOUNT_ID = '4a43aa969cbb4c56a5cb9bfbab4d1d7d';
const CLOUDFLARE_API_TOKEN = 'H4sIAAAAAAAAA2NkYGRkZBRiYWBgYPjHwMDA8A8dLgwUYWRkZPjHwMDA8A9dOAsYGBiYGBgYRoLIA4tQKQxkj2dkZGT4h6ocWjkYGBgYRsKPwsgI5RiJ2glQNRlRy0HqzMDAwPCPkZGR4R86mZQqBgYGBoZ/yNqMrJ2INQxYDYDVy8j2g2o3sn4g1jBgdQGqdiOrH1j9wGoB1GBgYGD4R6wZ0FogaQeqdiO7BqkLiLqAqAtIdQGyLiDuAlJdQKoLSLqAVBcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXEHcBcRcQdwFxFxB3AXEXELeA/8gCAGkAKQABAAA=';
const SUPABASE_URL = 'https://plqzygsrozwylyelhzue.supabase.co';

const cars = [
  {"id":21,"name":"New 2026 Hyundai PALISADE SEL Premium","make":"Hyundai","model":"PALISADE","year":2026,"price":2100,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":22,"name":"New 2026 Hyundai PALISADE Limited","make":"Hyundai","model":"PALISADE","year":2026,"price":2700,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":23,"name":"New 2026 Hyundai PALISADE Calligraphy","make":"Hyundai","model":"PALISADE","year":2026,"price":3300,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":24,"name":"New 2025 Hyundai PALISADE XRT Pro","make":"Hyundai","model":"PALISADE","year":2025,"price":3900,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":4,"name":"New 2026 Hyundai PALISADE Limited","make":"Hyundai","model":"PALISADE","year":2026,"price":3900,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":5,"name":"New 2026 Hyundai PALISADE SEL Premium","make":"Hyundai","model":"PALISADE","year":2026,"price":4500,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":6,"name":"New 2026 Hyundai PALISADE Calligraphy","make":"Hyundai","model":"PALISADE","year":2026,"price":3000,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":7,"name":"New 2026 Hyundai PALISADE XRT Pro","make":"Hyundai","model":"PALISADE","year":2026,"price":3600,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":8,"name":"New 2026 Hyundai PALISADE SEL","make":"Hyundai","model":"PALISADE","year":2026,"price":4200,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":9,"name":"New 2025 Hyundai PALISADE Limited","make":"Hyundai","model":"PALISADE","year":2025,"price":4800,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":10,"name":"New 2026 Hyundai PALISADE Limited","make":"Hyundai","model":"PALISADE","year":2026,"price":1500,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":11,"name":"New 2026 Hyundai PALISADE Calligraphy","make":"Hyundai","model":"PALISADE","year":2026,"price":2100,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":12,"name":"New 2026 Hyundai PALISADE XRT Pro","make":"Hyundai","model":"PALISADE","year":2026,"price":2700,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":13,"name":"New 2026 Hyundai PALISADE Limited","make":"Hyundai","model":"PALISADE","year":2026,"price":3300,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":14,"name":"New 2026 Hyundai PALISADE Calligraphy","make":"Hyundai","model":"PALISADE","year":2026,"price":3900,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":15,"name":"New 2026 Hyundai PALISADE XRT Pro","make":"Hyundai","model":"PALISADE","year":2026,"price":4500,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":16,"name":"New 2026 Hyundai PALISADE SEL Premium","make":"Hyundai","model":"PALISADE","year":2026,"price":3000,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":17,"name":"New 2026 Hyundai PALISADE Limited","make":"Hyundai","model":"PALISADE","year":2026,"price":3600,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":18,"name":"New 2026 Hyundai PALISADE Calligraphy","make":"Hyundai","model":"PALISADE","year":2026,"price":4200,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":19,"name":"New 2025 Hyundai PALISADE SEL","make":"Hyundai","model":"PALISADE","year":2025,"price":4800,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":20,"name":"New 2026 Hyundai PALISADE XRT Pro","make":"Hyundai","model":"PALISADE","year":2026,"price":1500,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":25,"name":"New 2026 Hyundai PALISADE SEL","make":"Hyundai","model":"PALISADE","year":2026,"price":4500,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":26,"name":"New 2026 Hyundai PALISADE Limited","make":"Hyundai","model":"PALISADE","year":2026,"price":3000,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":27,"name":"New 2026 Hyundai PALISADE Calligraphy","make":"Hyundai","model":"PALISADE","year":2026,"price":3600,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":28,"name":"New 2025 Hyundai PALISADE SEL Premium","make":"Hyundai","model":"PALISADE","year":2025,"price":4200,"body_style":"4D Sport Utility","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":29,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":4800,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":30,"name":"New 2026 Toyota Camry XSE","make":"Toyota","model":"Camry","year":2026,"price":1500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":31,"name":"New 2026 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2026,"price":2100,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":32,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":2700,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":33,"name":"New 2026 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2026,"price":3300,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":34,"name":"New 2026 Toyota Camry XSE","make":"Toyota","model":"Camry","year":2026,"price":3900,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":35,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":4500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":36,"name":"New 2026 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2026,"price":3000,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":37,"name":"New 2026 Toyota Camry XSE","make":"Toyota","model":"Camry","year":2026,"price":3600,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":38,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":4200,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":39,"name":"New 2025 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2025,"price":4800,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":40,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":1500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":41,"name":"New 2026 Toyota Camry XSE","make":"Toyota","model":"Camry","year":2026,"price":2100,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":42,"name":"New 2026 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2026,"price":2700,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":43,"name":"New 2025 Toyota Camry SE","make":"Toyota","model":"Camry","year":2025,"price":3300,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":44,"name":"New 2026 Toyota Camry XSE","make":"Toyota","model":"Camry","year":2026,"price":3900,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":45,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":4500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":46,"name":"New 2026 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2026,"price":3000,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":47,"name":"New 2025 Toyota Camry XSE","make":"Toyota","model":"Camry","year":2025,"price":3600,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":48,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":4200,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":49,"name":"New 2026 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2026,"price":4800,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":50,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":1500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":51,"name":"New 2025 Toyota Camry XLE","make":"Toyota","model":"Camry","year":2025,"price":2100,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":52,"name":"New 2026 Toyota Camry XSE","make":"Toyota","model":"Camry","year":2026,"price":2700,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":53,"name":"New 2026 Toyota Camry SE","make":"Toyota","model":"Camry","year":2026,"price":3300,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":54,"name":"New 2026 Honda CR-V EX","make":"Honda","model":"CR-V","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":55,"name":"New 2026 Honda CR-V EX-L","make":"Honda","model":"CR-V","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":56,"name":"New 2026 Honda CR-V Sport","make":"Honda","model":"CR-V","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":57,"name":"New 2025 Honda CR-V EX","make":"Honda","model":"CR-V","year":2025,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":58,"name":"New 2026 Honda CR-V EX-L","make":"Honda","model":"CR-V","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":59,"name":"New 2026 Honda CR-V Sport","make":"Honda","model":"CR-V","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":60,"name":"New 2025 Honda CR-V EX-L","make":"Honda","model":"CR-V","year":2025,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":61,"name":"New 2026 Honda CR-V EX","make":"Honda","model":"CR-V","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":62,"name":"New 2026 Honda Civic Sport","make":"Honda","model":"Civic","year":2026,"price":2700,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":63,"name":"New 2026 Honda Civic EX","make":"Honda","model":"Civic","year":2026,"price":3300,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":64,"name":"New 2026 Honda Civic Si","make":"Honda","model":"Civic","year":2026,"price":3900,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"6-Speed Manual"},
  {"id":65,"name":"New 2025 Honda Civic Sport","make":"Honda","model":"Civic","year":2025,"price":4500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":66,"name":"New 2026 Honda Civic EX-L","make":"Honda","model":"Civic","year":2026,"price":3000,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":67,"name":"New 2026 Ford F-150 XL","make":"Ford","model":"F-150","year":2026,"price":3600,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":68,"name":"New 2026 Ford F-150 XLT","make":"Ford","model":"F-150","year":2026,"price":4200,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":69,"name":"New 2026 Ford F-150 Lariat","make":"Ford","model":"F-150","year":2026,"price":4800,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":70,"name":"New 2025 Ford F-150 XL","make":"Ford","model":"F-150","year":2025,"price":1500,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":71,"name":"New 2026 Ford F-150 Platinum","make":"Ford","model":"F-150","year":2026,"price":2100,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":72,"name":"New 2026 Ford Explorer XLT","make":"Ford","model":"Explorer","year":2026,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":73,"name":"New 2026 Ford Explorer Limited","make":"Ford","model":"Explorer","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":74,"name":"New 2026 Ford Explorer ST","make":"Ford","model":"Explorer","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":75,"name":"New 2026 Chevrolet Tahoe LS","make":"Chevrolet","model":"Tahoe","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":76,"name":"New 2026 Chevrolet Tahoe Premier","make":"Chevrolet","model":"Tahoe","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":77,"name":"New 2025 Chevrolet Tahoe LS","make":"Chevrolet","model":"Tahoe","year":2025,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":78,"name":"New 2026 Chevrolet Silverado 1500 LT","make":"Chevrolet","model":"Silverado 1500","year":2026,"price":4200,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":79,"name":"New 2026 Chevrolet Silverado 1500 RST","make":"Chevrolet","model":"Silverado 1500","year":2026,"price":4800,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":80,"name":"New 2025 Chevrolet Silverado 1500 LT","make":"Chevrolet","model":"Silverado 1500","year":2025,"price":1500,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":81,"name":"New 2026 BMW X5 sDrive40i","make":"BMW","model":"X5","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":82,"name":"New 2026 BMW X5 xDrive40i","make":"BMW","model":"X5","year":2026,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":83,"name":"New 2026 BMW X5 M60i","make":"BMW","model":"X5","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":84,"name":"New 2025 BMW X5 sDrive40i","make":"BMW","model":"X5","year":2025,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":85,"name":"New 2026 BMW X5 xDrive40i","make":"BMW","model":"X5","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":86,"name":"New 2026 BMW 330i","make":"BMW","model":"3 Series","year":2026,"price":3000,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":87,"name":"New 2026 BMW 330i xDrive","make":"BMW","model":"3 Series","year":2026,"price":3600,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":88,"name":"New 2025 BMW 330i","make":"BMW","model":"3 Series","year":2025,"price":4200,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":89,"name":"New 2026 Mercedes-Benz GLE 350","make":"Mercedes-Benz","model":"GLE","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":90,"name":"New 2026 Mercedes-Benz GLE 450","make":"Mercedes-Benz","model":"GLE","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":91,"name":"New 2026 Mercedes-Benz GLE 580","make":"Mercedes-Benz","model":"GLE","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":92,"name":"New 2025 Mercedes-Benz GLE 350","make":"Mercedes-Benz","model":"GLE","year":2025,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":93,"name":"New 2026 Mercedes-Benz C 300","make":"Mercedes-Benz","model":"C-Class","year":2026,"price":3300,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":94,"name":"New 2026 Mercedes-Benz C 300 4MATIC","make":"Mercedes-Benz","model":"C-Class","year":2026,"price":3900,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":95,"name":"New 2025 Mercedes-Benz C 300","make":"Mercedes-Benz","model":"C-Class","year":2025,"price":4500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":96,"name":"New 2026 Tesla Model Y Long Range","make":"Tesla","model":"Model Y","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":97,"name":"New 2026 Tesla Model Y Performance","make":"Tesla","model":"Model Y","year":2026,"price":3600,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":98,"name":"New 2025 Tesla Model Y Long Range","make":"Tesla","model":"Model Y","year":2025,"price":4200,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":99,"name":"New 2026 Tesla Model Y RWD","make":"Tesla","model":"Model Y","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":100,"name":"New 2025 Tesla Model Y Performance","make":"Tesla","model":"Model Y","year":2025,"price":1500,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":101,"name":"New 2026 Tesla Model 3 Long Range","make":"Tesla","model":"Model 3","year":2026,"price":2100,"body_style":"Sedan","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":102,"name":"New 2026 Tesla Model 3 Performance","make":"Tesla","model":"Model 3","year":2026,"price":2700,"body_style":"Sedan","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":103,"name":"New 2025 Tesla Model 3 RWD","make":"Tesla","model":"Model 3","year":2025,"price":3300,"body_style":"Sedan","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":104,"name":"New 2026 Tesla Model 3 Long Range","make":"Tesla","model":"Model 3","year":2026,"price":3900,"body_style":"Sedan","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":105,"name":"New 2025 Tesla Model 3 Performance","make":"Tesla","model":"Model 3","year":2025,"price":4500,"body_style":"Sedan","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":106,"name":"New 2026 Jeep Grand Cherokee Laredo","make":"Jeep","model":"Grand Cherokee","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":107,"name":"New 2026 Jeep Grand Cherokee Limited","make":"Jeep","model":"Grand Cherokee","year":2026,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":108,"name":"New 2026 Jeep Grand Cherokee Overland","make":"Jeep","model":"Grand Cherokee","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":109,"name":"New 2026 Jeep Grand Cherokee Summit","make":"Jeep","model":"Grand Cherokee","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":110,"name":"New 2025 Jeep Grand Cherokee Laredo","make":"Jeep","model":"Grand Cherokee","year":2025,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":111,"name":"New 2026 Nissan Rogue S","make":"Nissan","model":"Rogue","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":112,"name":"New 2026 Nissan Rogue SL","make":"Nissan","model":"Rogue","year":2026,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":113,"name":"New 2026 Nissan Rogue Platinum","make":"Nissan","model":"Rogue","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":114,"name":"New 2025 Nissan Rogue S","make":"Nissan","model":"Rogue","year":2025,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":115,"name":"New 2026 Subaru Outback Premium","make":"Subaru","model":"Outback","year":2026,"price":4500,"body_style":"Wagon","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":116,"name":"New 2026 Subaru Outback Onyx Edition","make":"Subaru","model":"Outback","year":2026,"price":3000,"body_style":"Wagon","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":117,"name":"New 2026 Subaru Outback Touring","make":"Subaru","model":"Outback","year":2026,"price":3600,"body_style":"Wagon","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":118,"name":"New 2025 Subaru Outback Premium","make":"Subaru","model":"Outback","year":2025,"price":4200,"body_style":"Wagon","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":119,"name":"New 2026 Mazda CX-5 S","make":"Mazda","model":"CX-5","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":120,"name":"New 2026 Mazda CX-5 Select","make":"Mazda","model":"CX-5","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":121,"name":"New 2026 Mazda CX-5 Premium","make":"Mazda","model":"CX-5","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":122,"name":"New 2025 Mazda CX-5 S","make":"Mazda","model":"CX-5","year":2025,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":123,"name":"New 2026 Kia Telluride LX","make":"Kia","model":"Telluride","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":124,"name":"New 2026 Kia Telluride EX","make":"Kia","model":"Telluride","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":125,"name":"New 2026 Kia Telluride SX","make":"Kia","model":"Telluride","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":126,"name":"New 2026 Kia Telluride SX Prestige","make":"Kia","model":"Telluride","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":127,"name":"New 2025 Kia Telluride LX","make":"Kia","model":"Telluride","year":2025,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":128,"name":"New 2026 Kia Sportage LX","make":"Kia","model":"Sportage","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":129,"name":"New 2026 Kia Sportage EX","make":"Kia","model":"Sportage","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":130,"name":"New 2026 Kia Sportage SX","make":"Kia","model":"Sportage","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":131,"name":"New 2025 Kia Sportage LX","make":"Kia","model":"Sportage","year":2025,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":132,"name":"New 2026 Toyota RAV4 XLE","make":"Toyota","model":"RAV4","year":2026,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":133,"name":"New 2026 Toyota RAV4 XSE","make":"Toyota","model":"RAV4","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":134,"name":"New 2026 Toyota RAV4 Prime","make":"Toyota","model":"RAV4","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Hybrid","transmission":"CVT"},
  {"id":135,"name":"New 2026 Toyota Highlander XSE","make":"Toyota","model":"Highlander","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":136,"name":"New 2026 Toyota Highlander Platinum","make":"Toyota","model":"Highlander","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":137,"name":"New 2025 Toyota RAV4 XLE","make":"Toyota","model":"RAV4","year":2025,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":138,"name":"New 2026 Toyota Corolla Cross","make":"Toyota","model":"Corolla Cross","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":139,"name":"New 2026 Honda Pilot EX-L","make":"Honda","model":"Pilot","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":140,"name":"New 2026 Honda Pilot TrailSport","make":"Honda","model":"Pilot","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":141,"name":"New 2026 Honda Accord EX","make":"Honda","model":"Accord","year":2026,"price":2100,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":142,"name":"New 2026 Honda Accord Sport","make":"Honda","model":"Accord","year":2026,"price":2700,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"6-Speed Manual"},
  {"id":143,"name":"New 2025 Honda Pilot EX-L","make":"Honda","model":"Pilot","year":2025,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":144,"name":"New 2026 Hyundai Tucson SEL","make":"Hyundai","model":"Tucson","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":145,"name":"New 2026 Hyundai Tucson Limited","make":"Hyundai","model":"Tucson","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":146,"name":"New 2026 Hyundai Sonata SEL","make":"Hyundai","model":"Sonata","year":2026,"price":3000,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":147,"name":"New 2026 Hyundai Sonata N Line","make":"Hyundai","model":"Sonata","year":2026,"price":3600,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":148,"name":"New 2025 Hyundai Tucson SEL","make":"Hyundai","model":"Tucson","year":2025,"price":4200,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":149,"name":"New 2026 Ford Bronco Sport Big Bend","make":"Ford","model":"Bronco Sport","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":150,"name":"New 2026 Ford Bronco Sport Badlands","make":"Ford","model":"Bronco Sport","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":151,"name":"New 2026 Ford Edge ST","make":"Ford","model":"Edge","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"7-Speed Automatic"},
  {"id":152,"name":"New 2025 Ford Bronco Sport Big Bend","make":"Ford","model":"Bronco Sport","year":2025,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":153,"name":"New 2026 Kia EV6 Wind","make":"Kia","model":"EV6","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":154,"name":"New 2026 Kia EV6 GT","make":"Kia","model":"EV6","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":155,"name":"New 2026 Kia Carnival SX","make":"Kia","model":"Carnival","year":2026,"price":4500,"body_style":"Minivan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":156,"name":"New 2025 Kia EV6 Wind","make":"Kia","model":"EV6","year":2025,"price":3000,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":157,"name":"New 2026 Chevrolet Equinox RS","make":"Chevrolet","model":"Equinox","year":2026,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":158,"name":"New 2026 Chevrolet Traverse RS","make":"Chevrolet","model":"Traverse","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":159,"name":"New 2026 Chevrolet Blazer RS","make":"Chevrolet","model":"Blazer","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":160,"name":"New 2025 Chevrolet Equinox RS","make":"Chevrolet","model":"Equinox","year":2025,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":161,"name":"New 2026 BMW X3 xDrive30i","make":"BMW","model":"X3","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":162,"name":"New 2026 BMW X1 xDrive28i","make":"BMW","model":"X1","year":2026,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"7-Speed Automatic"},
  {"id":163,"name":"New 2026 Mercedes-Benz GLC 300","make":"Mercedes-Benz","model":"GLC","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":164,"name":"New 2026 Mercedes-Benz GLA 250","make":"Mercedes-Benz","model":"GLA","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":165,"name":"New 2026 Jeep Wrangler Sport","make":"Jeep","model":"Wrangler","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Manual"},
  {"id":166,"name":"New 2026 Jeep Wrangler Unlimited Rubicon","make":"Jeep","model":"Wrangler","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":167,"name":"New 2026 Nissan Pathfinder SL","make":"Nissan","model":"Pathfinder","year":2026,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":168,"name":"New 2026 Nissan Z Sport","make":"Nissan","model":"Z","year":2026,"price":4200,"body_style":"Coupe","fuel_type":"Gasoline","transmission":"6-Speed Manual"},
  {"id":169,"name":"New 2026 Subaru Forester Premium","make":"Subaru","model":"Forester","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":170,"name":"New 2026 Subaru Forester Wilderness","make":"Subaru","model":"Forester","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":171,"name":"New 2026 Mazda CX-50 Meridian","make":"Mazda","model":"CX-50","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":172,"name":"New 2026 Mazda CX-90 Premium","make":"Mazda","model":"CX-90","year":2026,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":173,"name":"New 2026 Audi Q5 Premium","make":"Audi","model":"Q5","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"7-Speed Automatic"},
  {"id":174,"name":"New 2026 Audi A4 Premium","make":"Audi","model":"A4","year":2026,"price":3900,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"7-Speed Automatic"},
  {"id":175,"name":"New 2026 Volkswagen Tiguan SE","make":"Volkswagen","model":"Tiguan","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":176,"name":"New 2026 Volkswagen Atlas SE","make":"Volkswagen","model":"Atlas","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":177,"name":"New 2026 Dodge Charger R/T","make":"Dodge","model":"Charger","year":2026,"price":3600,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":178,"name":"New 2026 Dodge Hornet R/T","make":"Dodge","model":"Hornet","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Hybrid","transmission":"6-Speed Automatic"},
  {"id":179,"name":"New 2026 Hyundai Santa Fe SEL","make":"Hyundai","model":"Santa Fe","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":203,"name":"New 2026 Tesla Model X","make":"Tesla","model":"Model X","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":180,"name":"New 2026 Hyundai Santa Fe Calligraphy","make":"Hyundai","model":"Santa Fe","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":181,"name":"New 2026 Toyota Tacoma SR5","make":"Toyota","model":"Tacoma","year":2026,"price":2100,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":182,"name":"New 2026 Toyota Tacoma TRD Off-Road","make":"Toyota","model":"Tacoma","year":2026,"price":2700,"body_style":"Pickup","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":188,"name":"New 2026 Jeep Grand Cherokee 4xe","make":"Jeep","model":"Grand Cherokee","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Hybrid","transmission":"8-Speed Automatic"},
  {"id":189,"name":"New 2026 Nissan Ariya Engage","make":"Nissan","model":"Ariya","year":2026,"price":4800,"body_style":"SUV","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":190,"name":"New 2026 Subaru Impreza RS","make":"Subaru","model":"Impreza","year":2026,"price":1500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":191,"name":"New 2026 Mazda3 Premium","make":"Mazda","model":"Mazda3","year":2026,"price":2100,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"6-Speed Automatic"},
  {"id":192,"name":"New 2026 Audi Q7 Premium","make":"Audi","model":"Q7","year":2026,"price":2700,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":193,"name":"New 2026 Volkswagen Atlas Cross Sport SE","make":"Volkswagen","model":"Atlas Cross Sport","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":194,"name":"New 2026 Mercedes-Benz E-Class E 350","make":"Mercedes-Benz","model":"E-Class","year":2026,"price":3900,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"9-Speed Automatic"},
  {"id":195,"name":"New 2026 BMW 5 Series 530i","make":"BMW","model":"5 Series","year":2026,"price":4500,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":196,"name":"New 2026 Porsche Cayenne","make":"Porsche","model":"Cayenne","year":2026,"price":3000,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":197,"name":"New 2026 Porsche Macan","make":"Porsche","model":"Macan","year":2026,"price":3600,"body_style":"SUV","fuel_type":"Gasoline","transmission":"7-Speed Automatic"},
  {"id":198,"name":"New 2026 Lexus RX 350","make":"Lexus","model":"RX","year":2026,"price":4200,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":199,"name":"New 2026 Lexus ES 350","make":"Lexus","model":"ES","year":2026,"price":4800,"body_style":"Sedan","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":200,"name":"New 2026 Acura MDX Technology","make":"Acura","model":"MDX","year":2026,"price":1500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"10-Speed Automatic"},
  {"id":201,"name":"New 2026 Volvo XC90 B6","make":"Volvo","model":"XC90","year":2026,"price":2100,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":202,"name":"New 2026 Tesla Model S","make":"Tesla","model":"Model S","year":2026,"price":2700,"body_style":"Sedan","fuel_type":"Electric","transmission":"1-Speed Automatic"},
  {"id":183,"name":"New 2026 Honda HR-V EX","make":"Honda","model":"HR-V","year":2026,"price":3300,"body_style":"SUV","fuel_type":"Gasoline","transmission":"CVT"},
  {"id":184,"name":"New 2026 Kia Sorento LX","make":"Kia","model":"Sorento","year":2026,"price":3900,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":185,"name":"New 2026 Kia Sorento SX","make":"Kia","model":"Sorento","year":2026,"price":4500,"body_style":"SUV","fuel_type":"Gasoline","transmission":"8-Speed Automatic"},
  {"id":186,"name":"New 2026 Ford Mustang GT","make":"Ford","model":"Mustang","year":2026,"price":3000,"body_style":"Coupe","fuel_type":"Gasoline","transmission":"6-Speed Manual"},
  {"id":187,"name":"New 2026 Chevrolet Camaro SS","make":"Chevrolet","model":"Camaro","year":2026,"price":3600,"body_style":"Coupe","fuel_type":"Gasoline","transmission":"6-Speed Manual"}
];

async function generateEmbedding(text) {
  const response = await fetch(`https://api.cloudflare.com/client/v4/accounts/${CLOUDFLARE_ACCOUNT_ID}/ai/run/@cf/baai/bge-m3`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${CLOUDFLARE_API_TOKEN}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ text: [text] })
  });
  const data = await response.json();
  return data.result.data[0];
}

async function insertVectors() {
  console.log(`Generating embeddings for ${cars.length} cars...`);
  
  const vectors = [];
  
  for (const car of cars) {
    const text = `${car.year} ${car.make} ${car.model} ${car.body_style} ${car.fuel_type} ${car.transmission} ${car.price} VND`;
    console.log(`Generating embedding for car ${car.id}: ${car.make} ${car.model}...`);
    
    const embedding = await generateEmbedding(text);
    
    vectors.push({
      id: String(car.id),
      values: embedding,
      metadata: {
        name: car.name,
        make: car.make,
        model: car.model,
        year: car.year,
        price: car.price,
        body_style: car.body_style,
        fuel_type: car.fuel_type,
        transmission: car.transmission
      }
    });
  }
  
  console.log('Inserting vectors into Vectorize...');
  
  const response = await fetch(`https://api.cloudflare.com/client/v4/accounts/${CLOUDFLARE_ACCOUNT_ID}/vectorize/v1/indexes/rentacar-cars/insert`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${CLOUDFLARE_API_TOKEN}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ vectors })
  });
  
  const data = await response.json();
  console.log('Vectorize insert result:', data);
  
  if (data.success) {
    console.log(`Successfully inserted ${vectors.length} vectors!`);
  } else {
    console.error('Failed to insert vectors:', data);
  }
}

insertVectors().catch(console.error);
