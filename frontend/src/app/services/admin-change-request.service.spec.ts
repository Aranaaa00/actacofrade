import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminChangeRequestService } from './admin-change-request.service';

describe('AdminChangeRequestService', () => {
  let service: AdminChangeRequestService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AdminChangeRequestService],
    });
    service = TestBed.inject(AdminChangeRequestService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('endpoints', () => {
    service.create({} as any).subscribe();
    expect(http.expectOne('/api/admin-change-requests').request.method).toBe('POST');
    service.findAll().subscribe();
    http.expectOne('/api/admin-change-requests').flush([]);
    service.findById(2).subscribe();
    http.expectOne('/api/admin-change-requests/2').flush({});
    service.findCandidates(2).subscribe();
    http.expectOne('/api/admin-change-requests/2/candidates').flush([]);
    service.approve(2, {} as any).subscribe();
    expect(http.expectOne('/api/admin-change-requests/2/approve').request.method).toBe('PATCH');
    service.reject(2).subscribe();
    expect(http.expectOne('/api/admin-change-requests/2/reject').request.method).toBe('PATCH');
  });
});
