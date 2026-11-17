import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface Hotel {
  id: number;
  name: string;
  city: string | null;
}

export interface ContractBase {
  hotelId: number;
  startDate: string;
  endDate: string;
  markupPercentage: string;
  currency: 'USD';
}

export interface RoomInput {
  name: string;
  ratePerAdultPerNight: string;
  roomCount: number;
  maxAdults: number;
}

export interface ContractSummary extends ContractBase {
  id: number;
  hotelName: string;
  city: string | null;
  roomTypeCount: number;
}

export interface ContractDetail extends ContractBase {
  id: number;
  hotelName: string;
  city: string | null;
  roomTypes: (RoomInput & { id: number })[];
}

export interface SearchInput {
  checkInDate: string;
  numberOfNights: number;
  rooms: { adults: number }[];
}

export type UnavailabilityReason = 'INSUFFICIENT_ROOMS' | 'OCCUPANCY_EXCEEDED';

export interface Offer {
  hotelId: number;
  hotelName: string;
  city: string | null;
  contractId: number;
  roomTypeId: number;
  roomType: string;
  availability: 'AVAILABLE' | 'NOT_AVAILABLE';
  reasons: UnavailabilityReason[];
  totalPrice: string | null;
  currency: 'USD';
}

export interface SearchResult {
  checkInDate: string;
  checkOutDate: string;
  results: Offer[];
}

@Injectable({ providedIn: 'root' })
export class TravelApi {
  private readonly http = inject(HttpClient);

  hotels() {
    return this.http.get<Hotel[]>('/api/hotels');
  }

  addHotel(input: { name: string; city: string }) {
    return this.http.post<Hotel>('/api/hotels', input);
  }

  contracts() {
    return this.http.get<ContractSummary[]>('/api/contracts');
  }

  contract(id: string) {
    return this.http.get<ContractDetail>(`/api/contracts/${encodeURIComponent(id)}`);
  }

  addContract(input: ContractBase & { roomTypes: RoomInput[] }) {
    return this.http.post<ContractDetail>('/api/contracts', input);
  }

  search(input: SearchInput) {
    return this.http.post<SearchResult>('/api/search', input);
  }
}

export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const fields = error.error?.fieldErrors;
    if (Array.isArray(fields) && fields.length) {
      return fields.map(field => `${field.field}: ${field.message}`).join(' ');
    }
    if (typeof error.error?.message === 'string') {
      return error.error.message;
    }
  }
  return fallback;
}
