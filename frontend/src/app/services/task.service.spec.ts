import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TaskService } from './task.service';
import { buildTask } from '../../testing/fixtures';

describe('TaskService', () => {
  let service: TaskService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), TaskService],
    });
    service = TestBed.inject(TaskService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('findByEventId / findById', () => {
    service.findByEventId(1).subscribe();
    http.expectOne('/api/events/1/tasks').flush([buildTask()]);
    service.findById(1, 5).subscribe();
    http.expectOne('/api/events/1/tasks/5').flush(buildTask());
  });

  it('create / update / delete', () => {
    service.create(1, { title: 't' }).subscribe();
    expect(http.expectOne('/api/events/1/tasks').request.method).toBe('POST');
    http.verify();
    service.update(1, 5, { title: 'x' }).subscribe();
    expect(http.expectOne('/api/events/1/tasks/5').request.method).toBe('PUT');
    http.verify();
    service.delete(1, 5).subscribe();
    expect(http.expectOne('/api/events/1/tasks/5').request.method).toBe('DELETE');
  });

  it('lifecycle transitions', () => {
    service.accept(1, 2).subscribe();
    http.expectOne('/api/events/1/tasks/2/accept').flush(buildTask());
    service.startPreparation(1, 2).subscribe();
    http.expectOne('/api/events/1/tasks/2/start-preparation').flush(buildTask());
    service.confirm(1, 2).subscribe();
    http.expectOne('/api/events/1/tasks/2/confirm').flush(buildTask());
    service.complete(1, 2).subscribe();
    http.expectOne('/api/events/1/tasks/2/complete').flush(buildTask());
    service.reset(1, 2).subscribe();
    http.expectOne('/api/events/1/tasks/2/reset').flush(buildTask());
    service.reject(1, 2, 'razón').subscribe();
    const req = http.expectOne('/api/events/1/tasks/2/reject');
    expect(req.request.body.rejectionReason).toBe('razón');
    req.flush(buildTask());
  });

  it('findMyTasks sends filtered params', () => {
    service.findMyTasks({ eventType: 'CABILDO', statusGroup: 'PENDING', search: 'q', page: 0, size: 10 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/my-tasks');
    expect(req.request.params.get('eventType')).toBe('CABILDO');
    expect(req.request.params.get('statusGroup')).toBe('PENDING');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
  });

  it('findMyTasks with no params', () => {
    service.findMyTasks({}).subscribe();
    http.expectOne((r) => r.url === '/api/my-tasks').flush({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 });
  });

  it('getMyTaskStats', () => {
    service.getMyTaskStats().subscribe();
    http.expectOne('/api/my-tasks/stats').flush({ pendingCount: 0, confirmedCount: 0, rejectedCount: 0 });
  });
});
