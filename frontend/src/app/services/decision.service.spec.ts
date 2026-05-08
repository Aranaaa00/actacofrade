import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DecisionService } from './decision.service';
import { buildDecision } from '../../testing/fixtures';

describe('DecisionService', () => {
  let service: DecisionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), DecisionService],
    });
    service = TestBed.inject(DecisionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CRUD endpoints', () => {
    service.findByEventId(1).subscribe();
    http.expectOne('/api/events/1/decisions').flush([buildDecision()]);
    service.findById(1, 2).subscribe();
    http.expectOne('/api/events/1/decisions/2').flush(buildDecision());
    service.create(1, { area: 'X', title: 'y' }).subscribe();
    expect(http.expectOne('/api/events/1/decisions').request.method).toBe('POST');
    service.update(1, 2, { title: 'z' }).subscribe();
    expect(http.expectOne('/api/events/1/decisions/2').request.method).toBe('PUT');
    service.delete(1, 2).subscribe();
    expect(http.expectOne('/api/events/1/decisions/2').request.method).toBe('DELETE');
    service.accept(1, 2).subscribe();
    http.expectOne('/api/events/1/decisions/2/accept').flush(buildDecision());
    service.reject(1, 2).subscribe();
    http.expectOne('/api/events/1/decisions/2/reject').flush(buildDecision());
  });
});
