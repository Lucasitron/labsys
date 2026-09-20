import type { Location, LocationsResult } from '$lib/types/stock';
import { stockFetch } from './request';

export async function fetchLocations(fetchFn: typeof fetch): Promise<LocationsResult> {
	return stockFetch<LocationsResult>('/stock/locations', {}, fetchFn);
}

export async function fetchLocationOptions(fetchFn: typeof fetch): Promise<Location[]> {
	const result = await stockFetch<LocationsResult>('/stock/locations', {}, fetchFn);
	return (result.locations ?? []).map((l) => ({ id: l.id, label: l.name }));
}