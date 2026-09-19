import homeHtml from '../content/home.html?raw';
import { htmlResponse } from '../lib/html-response';

export function GET() {
  return htmlResponse(homeHtml);
}
