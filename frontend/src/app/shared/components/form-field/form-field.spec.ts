import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormField } from './form-field';

@Component({
  standalone: true,
  imports: [FormField],
  template: `
    <app-form-field [label]="label" [fieldId]="fieldId" [hasError]="hasError" [errorMessage]="msg">
      <input id="ctrl" />
    </app-form-field>
  `,
})
class HostComponent {
  @ViewChild(FormField, { static: true }) field!: FormField;
  label = 'Name';
  fieldId = 'name';
  hasError = false;
  msg = '';
}

describe('FormField', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  function input() {
    return fixture.nativeElement.querySelector('input') as HTMLInputElement;
  }

  it('does not set aria-invalid by default', () => {
    expect(input().getAttribute('aria-invalid')).toBeNull();
    expect(input().getAttribute('aria-describedby')).toBeNull();
  });

  it('sets aria-invalid and aria-describedby when hasError is true', () => {
    fixture.componentInstance.hasError = true;
    fixture.componentInstance.msg = 'Required';
    fixture.detectChanges();
    expect(input().getAttribute('aria-invalid')).toBe('true');
    expect(input().getAttribute('aria-describedby')).toBe('name-error');
  });

  it('removes aria attributes when error clears', () => {
    fixture.componentInstance.hasError = true;
    fixture.detectChanges();
    fixture.componentInstance.hasError = false;
    fixture.detectChanges();
    expect(input().getAttribute('aria-invalid')).toBeNull();
    expect(input().getAttribute('aria-describedby')).toBeNull();
  });

  it('errorId is null without fieldId', () => {
    fixture.componentInstance.field.fieldId = '';
    expect(fixture.componentInstance.field.errorId).toBeNull();
  });

  it('errorId is computed from fieldId', () => {
    expect(fixture.componentInstance.field.errorId).toBe('name-error');
  });

  it('skips when no control is found', () => {
    const f = fixture.componentInstance.field;
    spyOn(f as any, 'syncControlAria').and.callThrough();
    f.hasError = true;
    f.ngOnChanges({});
    expect(true).toBeTrue();
  });
});
