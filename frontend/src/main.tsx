import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/handjet/latin-700.css';
import '@fontsource/handjet/latin-ext-700.css';
import '@fontsource/handjet/vietnamese-700.css';
import App from './App';
import './styles.css';

const container = document.getElementById('root');

if (!container) {
  throw new Error('Không tìm thấy phần tử gốc #root trong index.html.');
}

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
