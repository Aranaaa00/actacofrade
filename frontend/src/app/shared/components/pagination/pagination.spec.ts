import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Pagination } from './pagination';

describe('Pagination', () => {
  let fixture: ComponentFixture<Pagination>;
  let p: Pagination;
  let pagesEmitted: number[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Pagination] }).compileComponents();
    fixture = TestBed.createComponent(Pagination);
    p = fixture.componentInstance;
    pagesEmitted = [];
    p.pageChanged.subscribe((n) => pagesEmitted.push(n));
  });

  it('builds the pages array based on totalPages', () => {
    p.totalPages = 4;
    expect(p.pages).toEqual([1, 2, 3, 4]);
  });

  it('emits when navigating to a valid page', () => {
    p.totalPages = 3;
    p.goToPage(2);
    expect(pagesEmitted).toEqual([2]);
  });

  it('does not emit when target page is out of bounds', () => {
    p.totalPages = 3;
    p.goToPage(0);
    p.goToPage(4);
    p.goToPage(-1);
    expect(pagesEmitted).toEqual([]);
  });
});
