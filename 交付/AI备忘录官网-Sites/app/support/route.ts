import supportHtml from '../../content/support.html?raw';
import { htmlResponse } from '../../lib/html-response';

export function GET() {
  return htmlResponse(supportHtml);
}
