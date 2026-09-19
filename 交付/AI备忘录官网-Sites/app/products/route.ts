import productHtml from '../../content/product.html?raw';
import { htmlResponse } from '../../lib/html-response';

export function GET() {
  return htmlResponse(productHtml);
}
