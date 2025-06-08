import React from 'react';
import type { Employee } from '../types';

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
}) => {  const isSelected = (employeeId: string) => 
    selectedEmployees.some(emp => emp.id === employeeId);

  const allSelected = employees.length > 0 && selectedEmployees.length === employees.length;
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

  return (
    <div className="bg-white shadow-sm rounded-lg overflow-hidden">
      <div className="px-4 py-3 border-b border-gray-200 flex justify-between items-center">
        <h3 className="text-lg font-medium text-gray-900">
          Employee Selection ({employees.length} total, {selectedEmployees.length} selected)
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
      
      <div className="overflow-x-auto max-h-96">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50 sticky top-0">
            <tr>
              <th className="px-3 py-3 text-left">
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
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Name
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Email
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Department
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Position
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Hire Date
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {employees.map((employee) => (
              <tr 
                key={employee.id}
                className={`hover:bg-gray-50 cursor-pointer ${
                  isSelected(employee.id) ? 'bg-blue-50' : ''
                }`}
                onClick={() => handleEmployeeToggle(employee)}
              >
                <td className="px-3 py-4">
                  <input
                    type="checkbox"
                    checked={isSelected(employee.id)}
                    onChange={() => handleEmployeeToggle(employee)}
                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {employee.firstName} {employee.lastName}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {employee.email}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {employee.department}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {employee.position}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {new Date(employee.hireDate).toLocaleDateString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default EmployeeTable;
