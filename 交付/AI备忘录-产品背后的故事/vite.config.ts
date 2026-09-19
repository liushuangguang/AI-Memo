import vinext from 'vinext';
import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/postcss';
export default defineConfig({css:{postcss:{plugins:[tailwindcss()]}},plugins:[vinext()]});
