import React, { useState, useMemo } from 'react';
import type { Employee } from '../types';
import EmployeeFilters from './EmployeeFilters';

interface EmployeeTableProps {
  employees: Employee[];
  selectedEmployees: Employee[];
  onEmployeeSelect: (employee: Employee) => void;
  onEmployeeDeselect: (employeeId: string) => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
}

const EmployeeTable: React.FC<EmployeeTableProps> = ({
  employees,
  selectedEmployees,
  onEmployeeSelect,
  onEmployeeDeselect,
  onSelectAll,
  onClearSelection
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');

  // Get unique departments
  const departments = useMemo(() => {
    const depts = employees.map(emp => emp.department);
    return Array.from(new Set(depts)).sort();
  }, [employees]);

  // Filter employees based on search and department filter
  const filteredEmployees = useMemo(() => {
    return employees.filter(employee => {
      const matchesSearch = searchTerm === '' || 
        employee.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        employee.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        employee.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        employee.position.toLowerCase().includes(searchTerm.toLowerCase());
      
      const matchesDepartment = departmentFilter === '' || employee.department === departmentFilter;
      
      return matchesSearch && matchesDepartment;
    });
  }, [employees, searchTerm, departmentFilter]);

  const isSelected = (employeeId: string) => 
    selectedEmployees.some(emp => emp.id === employeeId);
  const allSelected = filteredEmployees.length > 0 && selectedEmployees.length === employees.length;
  const someSelected = selectedEmployees.length > 0 && selectedEmployees.length < employees.length;

  const handleSelectAll = () => {
    if (allSelected) {
      onClearSelection();
    } else {
      onSelectAll();
    }
  };

  const handleEmployeeToggle = (employee: Employee) => {
    if (isSelected(employee.id)) {
      onEmployeeDeselect(employee.id);
    } else {
      onEmployeeSelect(employee);
    }
  };

  return (    <div className="bg-white shadow-sm rounded-lg overflow-hidden">
      <div className="px-4 py-3 border-b border-gray-200">
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-medium text-gray-900">
            Employee Selection ({filteredEmployees.length} of {employees.length} employees, {selectedEmployees.length} selected)
          </h3>
          <div className="flex gap-2">
            <button
              onClick={onSelectAll}
              className="text-sm text-blue-600 hover:text-blue-700"
            >
              Select All
            </button>
            <button
              onClick={onClearSelection}
              className="text-sm text-gray-600 hover:text-gray-700"
            >
              Clear Selection
            </button>
          </div>
        </div>
        
        <EmployeeFilters
          searchTerm={searchTerm}
          onSearchChange={setSearchTerm}
          departmentFilter={departmentFilter}
          onDepartmentFilterChange={setDepartmentFilter}
          departments={departments}
        />      </div>
      <div className="overflow-y-auto max-h-80">
        <table className="w-full divide-y divide-gray-200"><thead className="bg-gray-50 sticky top-0"><tr>
              <th className="px-3 py-3 text-left w-10">
                <input
                  type="checkbox"
                  checked={allSelected}
                  ref={input => {
                    if (input) input.indeterminate = someSelected;
                  }}
                  onChange={handleSelectAll}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Name
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden sm:table-cell">
                Email
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">
                Department
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">
                Position
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden xl:table-cell">
                Hire Date              </th>
            </tr></thead><tbody className="bg-white divide-y divide-gray-200">
            {filteredEmployees.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
                  <div>
                    <svg className="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <div className="text-lg font-medium text-gray-900 mb-2">No employees found</div>
                    <div className="text-sm text-gray-500">
                      {searchTerm || departmentFilter 
                        ? "Try adjusting your search or filter criteria."
                        : "No employees available."
                      }
                    </div>
                  </div>
                </td>
              </tr>
            ) : (
              filteredEmployees.map((employee) => (
              <tr 
                key={employee.id}
                className={`hover:bg-gray-50 cursor-pointer ${
                  isSelected(employee.id) ? 'bg-blue-50' : ''
                }`}
                onClick={() => handleEmployeeToggle(employee)}
              >
                <td className="px-3 py-3 w-10">
                  <input
                    type="checkbox"
                    checked={isSelected(employee.id)}
                    onChange={() => handleEmployeeToggle(employee)}
                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                </td>
                <td className="px-4 py-3 text-sm">
                  <div className="font-medium text-gray-900">
                    {employee.firstName} {employee.lastName}
                  </div>
                  <div className="text-gray-500 text-xs sm:hidden">
                    {employee.email}
                  </div>
                  <div className="text-gray-500 text-xs md:hidden">
                    {employee.department} • {employee.position}
                  </div>
                </td>
                <td className="px-4 py-3 text-sm text-gray-500 hidden sm:table-cell">
                  {employee.email}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500 hidden md:table-cell">
                  {employee.department}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500 hidden lg:table-cell">
                  {employee.position}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500 hidden xl:table-cell">
                  {new Date(employee.hireDate).toLocaleDateString()}                </td>
              </tr>            ))
            )}
          </tbody></table>
      </div>
    </div>
  );
};

export default EmployeeTable;
