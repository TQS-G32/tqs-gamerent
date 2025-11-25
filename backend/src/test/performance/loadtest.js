import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '5s', target: 10 },
    { duration: '10s', target: 50 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8081';

export default function () {
  // Search items
  const res = http.get(`${BASE_URL}/api/items/search?q=test`);

  const result = check(res, {
    'status is 200': (r) => r.status === 200,
    'content type is json': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
  });

  errorRate.add(!result);
  sleep(1);
}
