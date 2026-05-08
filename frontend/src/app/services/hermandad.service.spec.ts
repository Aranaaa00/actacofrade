import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HermandadService } from './hermandad.service';

describe('HermandadService', () => {
  let service: HermandadService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), HermandadService],
    });
    service = TestBed.inject(HermandadService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('endpoints', () => {
    service.getCurrent().subscribe();
    http.expectOne('/api/hermandades/me').flush({});
    service.updateCurrent({} as any).subscribe();
    expect(http.expectOne('/api/hermandades/me').request.method).toBe('PUT');
    service.deleteCurrent().subscribe();
    expect(http.expectOne('/api/hermandades/me').request.method).toBe('DELETE');
  });
});
