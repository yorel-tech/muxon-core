'use client';

import { motion } from 'framer-motion';
import { forwardRef, useState, type ReactNode } from 'react';
import { ChevronDown, ChevronUp, MoreHorizontal, Search, Filter } from 'lucide-react';

export type Column<T> = {
  key: string;
  header: string;
  cell: (row: T) => ReactNode;
  sortable?: boolean;
};

export interface TableProps {
  columns: Column<any>[];
  data: any[];
  onRowClick?: (row: any) => void;
  onSort?: (column: string, direction: 'asc' | 'desc') => void;
  sortColumn?: string;
  sortDirection?: 'asc' | 'desc';
  onFilter?: (query: string) => void;
  filterQuery?: string;
  emptyMessage?: string;
  isLoading?: boolean;
  className?: string;
  overflowVisibleColumnKeys?: string[];
}

// Helper function to safely get string representation of any value for filtering
function getFilterableValue(value: unknown): string {
  if (typeof value === 'string') return value as string;
  if (typeof value === 'number') return String(value);
  if (typeof value === 'boolean') return String(value);
  if (value === null || value === undefined) return '';
  return String(value);
}

function TableComponent({
  columns,
  data,
  onRowClick,
  onSort,
  sortColumn,
  sortDirection = 'asc',
  onFilter,
  filterQuery = '',
  emptyMessage = 'No data available',
  isLoading = false,
  className = '',
  overflowVisibleColumnKeys = [],
}: TableProps,
  ref: React.Ref<HTMLTableElement>,
) {
  const [localSortColumn, setLocalSortColumn] = useState<string | null>(null);
  const [localSortDirection, setLocalSortDirection] = useState<'asc' | 'desc'>('asc');

  const handleSort = (columnKey: string) => {
    if (onSort) {
      const newDirection = localSortColumn === columnKey && localSortDirection === 'asc' ? 'desc' : 'asc';
      setLocalSortColumn(columnKey);
      setLocalSortDirection(newDirection);
      onSort(columnKey, newDirection);
    }
  };

  const handleFilter = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (onFilter) {
      onFilter(event.target.value);
    }
  };

  const filteredData = filterQuery
    ? data.filter((row) => {
        // Convert row to object for Object.values()
        const rowObj = row as Record<string, unknown>;
        const rowValues = Object.values(rowObj);
        return rowValues.some((value) =>
          getFilterableValue(value).toLowerCase().includes(filterQuery.toLowerCase()),
        );
      })
    : data;

  const sortedData = localSortColumn
    ? [...filteredData].sort((a, b) => {
        const aObj = a as Record<string, unknown>;
        const bObj = b as Record<string, unknown>;
        const aValue = aObj[localSortColumn] ?? '';
        const bValue = bObj[localSortColumn] ?? '';
        const comparison = String(aValue ?? '').localeCompare(String(bValue ?? ''));
        return localSortDirection === 'asc' ? comparison : -comparison;
      })
    : filteredData;

  return (
    <div className={`overflow-x-auto ${className}`}>
      {/* Filter Bar */}
      <div className="flex items-center gap-4 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
          <input
            type="text"
            placeholder="Filter..."
            value={filterQuery}
            onChange={handleFilter}
            className="w-full pl-10 pr-10 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <button className="flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900">
          <Filter size={16} />
          <span>Filter</span>
        </button>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg border border-gray-200">
        <table ref={ref} className="w-full text-left">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              {columns.map((column) => (
                <th
                  key={column.key}
                  className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  <button
                    onClick={() => handleSort(column.key)}
                    className="flex items-center gap-1 group hover:text-gray-700 transition-colors"
                  >
                    {column.header}
                    {column.sortable && (
                      <motion.span
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ duration: 0.2 }}
                      >
                        {localSortColumn === column.key ? (
                          localSortDirection === 'asc' ? (
                            <ChevronUp size={14} />
                          ) : (
                            <ChevronDown size={14} />
                          )
                        ) : (
                          <MoreHorizontal size={14} />
                        )}
                      </motion.span>
                    )}
                  </button>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={columns.length} className="px-3 py-4 text-center text-sm text-gray-500">
                  Loading...
                </td>
              </tr>
            ) : sortedData.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-3 py-8 text-center text-sm text-gray-500">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              sortedData.map((row, index) => (
                <motion.tr
                  key={index}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.2 }}
                  onClick={() => onRowClick?.(row)}
                  className="group cursor-pointer hover:bg-gray-50 transition-colors"
                >
                  {columns.map((column) => (
                    <td key={column.key} className={`px-3 py-3 ${overflowVisibleColumnKeys.includes(column.key) ? 'overflow-visible' : 'whitespace-nowrap'}`}>
                      {column.cell(row)}
                    </td>
                  ))}
                </motion.tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export const Table = forwardRef(TableComponent);
Table.displayName = 'Table';
