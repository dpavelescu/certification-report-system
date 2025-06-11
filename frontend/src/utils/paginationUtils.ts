
export interface PaginationResult<T> {
  totalItems: number;
  totalPages: number;
  startIndex: number;
  endIndex: number;
  paginatedItems: T[];
}

/**
 * Helper function to calculate pagination for arrays
 */
export function calculatePagination<T>(
  items: T[], 
  currentPage: number, 
  itemsPerPage: number
): PaginationResult<T> {
  const totalItems = items.length;
  const totalPages = Math.ceil(totalItems / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedItems = items.slice(startIndex, endIndex);
  
  return { totalItems, totalPages, startIndex, endIndex, paginatedItems };
}
