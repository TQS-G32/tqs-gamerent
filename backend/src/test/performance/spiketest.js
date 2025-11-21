import http from 'k6/http';
import { check, sleep } from 'k6';

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

const BASE_URL = 'http://localhost:8080';

export default function () {

}