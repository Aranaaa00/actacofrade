import { ROLES_ALL, ROLES_MANAGE, ROLES_WRITE, ROLES_ADMIN, ROLES_SUPER_ADMIN, ROLE_LABELS, ROLE_BADGE_VARIANTS } from './roles.const';
import { AREA_OPTIONS } from './areas.const';

describe('roles.const', () => {
  it('declares disjoint and consistent role groups', () => {
    expect(ROLES_ADMIN).toContain('ADMINISTRADOR');
    expect(ROLES_MANAGE).toEqual(jasmine.arrayContaining(['ADMINISTRADOR', 'RESPONSABLE']));
    expect(ROLES_WRITE.length).toBeGreaterThanOrEqual(ROLES_MANAGE.length);
    expect(ROLES_ALL).toContain('CONSULTA');
    expect(ROLES_SUPER_ADMIN).toEqual(['SUPER_ADMIN']);
  });

  it('provides labels for every role', () => {
    ['SUPER_ADMIN', 'ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR', 'CONSULTA'].forEach((r) => {
      expect(ROLE_LABELS[r]).toBeTruthy();
      expect(ROLE_BADGE_VARIANTS[r]).toBeTruthy();
    });
  });
});

describe('areas.const', () => {
  it('exposes a non-empty list of area options', () => {
    expect(AREA_OPTIONS.length).toBeGreaterThan(0);
    AREA_OPTIONS.forEach((opt) => {
      expect(opt.value).toBeTruthy();
      expect(opt.label).toBeTruthy();
    });
  });
});
