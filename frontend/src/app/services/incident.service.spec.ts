import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { IncidentService } from './incident.service';
import { buildIncident } from '../../testing/fixtures';

describe('IncidentService', () => {
  let service: IncidentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), IncidentService],
    });
    service = TestBed.inject(IncidentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CRUD endpoints', () => {
    service.findByEventId(1).subscribe();
    http.expectOne('/api/events/1/incidents').flush([buildIncident()]);
    service.findById(1, 2).subscribe();
    http.expectOne('/api/events/1/incidents/2').flush(buildIncident());
    service.create(1, { description: 'd' }).subscribe();
    expect(http.expectOne('/api/events/1/incidents').request.method).toBe('POST');
    service.delete(1, 2).subscribe();
    expect(http.expectOne('/api/events/1/incidents/2').request.method).toBe('DELETE');
    service.resolve(1, 2).subscribe();
    http.expectOne('/api/events/1/incidents/2/resolve').flush(buildIncident());
  });
});
