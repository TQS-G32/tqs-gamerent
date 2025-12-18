import { check, sleep } from 'k6';
import http from 'k6/http';

// Stress test: find breaking point
export const options = {
  stages: [
    // ramp up from 0 to 50 VUs over the next 30 seconds
    { duration: '30s', target: 50 },  // Ramp up to moderate load
    // run 50 VUs over the next 1 minute
    { duration: '1m', target: 50 },   // Stay at 50
    // ramp up from 50 to 100 VUs over the next 30 seconds
    { duration: '30s', target: 100 }, // Increase load
    // run 100 VUs over the next 1 minute
    { duration: '1m', target: 100 },  // Stay at 100
    // ramp down from 100 to 0 VUs over the next 30 seconds
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(99)<1000'], // 99% of requests should be < 1s
  },
};

const BASE_URL = 'http://localhost:8081';

export default function () {
  const response = http.get(`${BASE_URL}/api/items/catalog?page=0`);
  
  check(response, {
    'status is 200': (r) => r.status === 200,
  });
  
  sleep(1);
}