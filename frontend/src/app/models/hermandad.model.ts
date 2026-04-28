export interface HermandadResponse {
  id: number;
  nombre: string;
  descripcion: string | null;
  anioFundacion: number | null;
  localidad: string | null;
  direccionSede: string | null;
  emailContacto: string | null;
  telefonoContacto: string | null;
  sitioWeb: string | null;
  createdAt: string;
  updatedAt: string | null;
  usersCount: number;
  eventsCount: number;
}

export interface HermandadUpdateRequest {
  nombre: string;
  descripcion: string | null;
  anioFundacion: number | null;
  localidad: string | null;
  direccionSede: string | null;
  emailContacto: string | null;
  telefonoContacto: string | null;
  sitioWeb: string | null;
}
