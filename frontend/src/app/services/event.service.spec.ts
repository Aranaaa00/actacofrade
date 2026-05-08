import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EventService } from './event.service';
import { buildEvent } from '../../testing/fixtures';

describe('EventService', () => {
  let service: EventService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), EventService],
    });
    service = TestBed.inject(EventService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('findAll', () => {
    service.findAll().subscribe();
    http.expectOne('/api/events').flush([buildEvent()]);
  });

  it('filter sends only provided params', () => {
    service.filter({ eventType: 'CABILDO', search: 'x', page: 1, size: 10 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/events/filter');
    expect(req.request.params.get('eventType')).toBe('CABILDO');
    expect(req.request.params.get('search')).toBe('x');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.has('status')).toBe(false);
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
  });

  it('filter with no params still calls endpoint', () => {
    service.filter({}).subscribe();
    const req = http.expectOne((r) => r.url === '/api/events/filter');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 });
  });

  it('filter sends status and eventDate when provided alone', () => {
    service.filter({ status: 'PENDIENTE', eventDate: '2025-05-08' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/events/filter');
    expect(req.request.params.get('status')).toBe('PENDIENTE');
    expect(req.request.params.get('eventDate')).toBe('2025-05-08');
    expect(req.request.params.has('search')).toBeFalse();
    expect(req.request.params.has('page')).toBeFalse();
    expect(req.request.params.has('size')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 });
  });

  it('history with no params calls the endpoint', () => {
    service.history({}).subscribe();
    const req = http.expectOne((r) => r.url === '/api/events/history');
    expect(req.request.params.keys().length).toBe(0);
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 });
  });

  it('history sends every provided param', () => {
    service.history({ eventType: 'CULTOS', responsibleId: 4, dateFrom: '2025-01-01', dateTo: '2025-12-31', search: 'q', page: 0, size: 5 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/events/history');
    expect(req.request.params.get('responsibleId')).toBe('4');
    expect(req.request.params.get('dateFrom')).toBe('2025-01-01');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 5, number: 0 });
  });

  it('history with empty params', () => {
    service.history({}).subscribe();
    http.expectOne((r) => r.url === '/api/events/history').flush({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 });
  });

  it('availableDates', () => {
    service.availableDates().subscribe();
    http.expectOne('/api/events/available-dates').flush([]);
  });

  it('findById', () => {
    service.findById(7).subscribe();
    http.expectOne('/api/events/7').flush(buildEvent({ id: 7 }));
  });

  it('create', () => {
    service.create({ title: 't', eventType: 'CABILDO', eventDate: '2025-01-01' }).subscribe();
    const req = http.expectOne('/api/events');
    expect(req.request.method).toBe('POST');
    req.flush(buildEvent());
  });

  it('update', () => {
    service.update(3, { title: 't' }).subscribe();
    const req = http.expectOne('/api/events/3');
    expect(req.request.method).toBe('PUT');
    req.flush(buildEvent());
  });

  it('delete', () => {
    service.delete(5).subscribe();
    const req = http.expectOne('/api/events/5');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('close / advanceStatus / clone', () => {
    service.close(1).subscribe();
    http.expectOne('/api/events/1/close').flush(buildEvent());
    service.advanceStatus(1).subscribe();
    http.expectOne('/api/events/1/advance-status').flush(buildEvent());
    service.clone(1).subscribe();
    http.expectOne('/api/events/1/clone').flush(buildEvent());
  });

  it('export returns blob', () => {
    service.export(2, 'PDF', ['TASKS']).subscribe((blob) => expect(blob).toBeTruthy());
    const req = http.expectOne('/api/events/2/export');
    expect(req.request.body.format).toBe('PDF');
    req.flush(new Blob(['x']));
  });
});
