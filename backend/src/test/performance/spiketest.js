import { check, sleep } from 'k6';
import http from 'k6/http';

// Spike test: sudden load increase
export const options = {
  stages: [
    // ramp up from 0 to 10 VUs over the next 10 seconds
    { duration: '10s', target: 10 },   // Normal load
    // spike to 100 VUs over the next 10 seconds
    { duration: '10s', target: 100 },  // Sudden spike
    { duration: '10s', target: 10 },
  ],
};

const BASE_URL = 'http://localhost:8081';

export default function () {
  const response = http.get(`${BASE_URL}/api/items/catalog?page=0`);
  
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 1s': (r) => r.timings.duration < 1000,
  });

  sleep(1);
}