import { htmlResponse, maintenancePage } from '../../lib/html-response';

export function GET() {
  return htmlResponse(maintenancePage('download'));
}
