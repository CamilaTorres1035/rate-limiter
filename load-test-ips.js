import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // 20 Usuarios Virtuales ejecutando peticiones durante 10 segundos
  vus: 20,
  duration: '10s',
};

// Generador auxiliar de IPs aleatorias estilo IPv4 (ej. 192.168.1.45)
function generateRandomIp() {
  const p1 = Math.floor(Math.random() * 255) + 1;
  const p2 = Math.floor(Math.random() * 255);
  const p3 = Math.floor(Math.random() * 255);
  const p4 = Math.floor(Math.random() * 255) + 1;
  return `${p1}.${p2}.${p3}.${p4}`;
}

export default function () {
  // Opción A: IP aleatoria por cada petición individual
  const fakeIp = generateRandomIp();

  // Opción B (Comentada): IP fija por cada Usuario Virtual (VU)
  // const fakeIp = `192.168.1.${__VU}`;

  const params = {
    headers: {
      'X-Forwarded-For': fakeIp,
      'Content-Type': 'application/json',
    },
  };

  const res = http.get('http://localhost:8080/ping', params);

  check(res, {
    'Status 200 (IP única permitida)': (r) => r.status === 200,
    'Contiene encabezado X-RateLimit-Limit': (r) => r.headers['X-Ratelimit-Limit'] !== undefined,
  });

  sleep(0.05); // Pausa de 50 ms entre peticiones
}