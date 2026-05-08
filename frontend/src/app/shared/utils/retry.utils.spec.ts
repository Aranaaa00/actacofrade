import { fakeAsync, tick } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of, defer } from 'rxjs';
import { retryReads } from './retry.utils';

describe('retryReads', () => {
  it('passes through successful values without retrying', (done) => {
    of('value').pipe(retryReads()).subscribe({
      next: (v) => expect(v).toBe('value'),
      complete: done,
    });
  });

  it('retries on retryable status and eventually succeeds', fakeAsync(() => {
    let attempts = 0;
    const source$ = defer(() => {
      attempts += 1;
      if (attempts < 3) {
        return throwError(() => new HttpErrorResponse({ status: 503 }));
      }
      return of('ok');
    });
    let value: string | undefined;
    source$.pipe(retryReads(3, 10)).subscribe((v) => (value = v));
    tick(100);
    expect(attempts).toBe(3);
    expect(value).toBe('ok');
  }));

  it('does not retry on non-retryable status', fakeAsync(() => {
    let attempts = 0;
    const source$ = defer(() => {
      attempts += 1;
      return throwError(() => new HttpErrorResponse({ status: 404 }));
    });
    let error: HttpErrorResponse | null = null;
    source$.pipe(retryReads(3, 10)).subscribe({ error: (e) => (error = e) });
    tick(100);
    expect(attempts).toBe(1);
    expect(error!.status).toBe(404);
  }));

  it('stops retrying after max attempts', fakeAsync(() => {
    let attempts = 0;
    const source$: Observable<string> = defer(() => {
      attempts += 1;
      return throwError(() => new HttpErrorResponse({ status: 503 }));
    });
    let error: HttpErrorResponse | null = null;
    source$.pipe(retryReads(2, 10)).subscribe({ error: (e) => (error = e) });
    tick(1000);
    expect(attempts).toBe(3);
    expect(error!.status).toBe(503);
  }));

  it('treats non-HttpErrorResponse as status 0 (retryable)', fakeAsync(() => {
    let attempts = 0;
    const source$ = defer(() => {
      attempts += 1;
      if (attempts === 1) {
        return throwError(() => new Error('network'));
      }
      return of('ok');
    });
    let value: string | undefined;
    source$.pipe(retryReads(2, 10)).subscribe((v) => (value = v));
    tick(100);
    expect(value).toBe('ok');
  }));
});
