import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // Simular 10 usuarios concurrentes haciendo peticiones durante 10 segundos
  vus: 10,
  duration: '10s',
};

export default function () {
  // Petición al endpoint /ping
  const res = http.get('http://localhost:8080/ping');

  // Validar que la respuesta sea 200 (Permitida) o 429 (Limitada)
  check(res, {
    'status es 200 o 429': (r) => r.status === 200 || r.status === 429,
    'retorna encabezado X-RateLimit-Limit': (r) => r.headers['X-Ratelimit-Limit'] !== undefined,
  });

  // Pequeña pausa entre iteraciones
  sleep(0.1); // 100 ms
}