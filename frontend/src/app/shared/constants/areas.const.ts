export interface AreaOption {
  readonly value: string;
  readonly label: string;
}

export const AREA_OPTIONS: readonly AreaOption[] = [
  { value: 'MAYORDOMIA', label: 'Mayordomía' },
  { value: 'SECRETARIA', label: 'Secretaría' },
  { value: 'PRIOSTIA', label: 'Priostía' },
  { value: 'TESORERIA', label: 'Tesorería' },
  { value: 'DIPUTACION_MAYOR', label: 'Diputación Mayor' },
];
