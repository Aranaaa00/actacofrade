import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserService } from './user.service';
import { buildUser } from '../../testing/fixtures';

describe('UserService', () => {
  let service: UserService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), UserService],
    });
    service = TestBed.inject(UserService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CRUD endpoints', () => {
    service.findAll().subscribe();
    http.expectOne('/api/users').flush([buildUser()]);
    service.findAssignable().subscribe();
    http.expectOne('/api/users/assignable').flush([buildUser()]);
    service.findById(2).subscribe();
    http.expectOne('/api/users/2').flush(buildUser());
    service.getStats().subscribe();
    http.expectOne('/api/users/stats').flush({ administradores: 1, responsables: 0, colaboradores: 0, consulta: 0 });
    service.create({ fullName: 'x', email: 'a@b.c', password: 'p', roleCode: 'COLABORADOR' }).subscribe();
    expect(http.expectOne('/api/users').request.method).toBe('POST');
    service.update(3, { fullName: 'y' }).subscribe();
    expect(http.expectOne('/api/users/3').request.method).toBe('PUT');
    service.toggleActive(3).subscribe();
    http.expectOne('/api/users/3/toggle-active').flush(buildUser());
    service.delete(3).subscribe();
    expect(http.expectOne('/api/users/3').request.method).toBe('DELETE');
  });
});
