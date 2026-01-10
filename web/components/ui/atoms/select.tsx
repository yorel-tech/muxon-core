'use client';

import { motion } from 'framer-motion';
import { forwardRef, useState, type ReactNode, type ChangeEvent, type FocusEvent } from 'react';
import { ChevronDown, Check } from 'lucide-react';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface SelectProps {
  label?: string;
  error?: string;
  helperText?: string;
  options: SelectOption[];
  value?: string;
  defaultValue?: string;
  onChange?: (value: string) => void;
  onFocus?: (event: FocusEvent<HTMLSelectElement>) => void;
  onBlur?: (event: FocusEvent<HTMLSelectElement>) => void;
  disabled?: boolean;
  placeholder?: string;
  fullWidth?: boolean;
  size?: 'sm' | 'md' | 'lg';
  name?: string;
  id?: string;
  'aria-label'?: string;
  'aria-invalid'?: boolean;
  className?: string;
}

const sizeStyles: Record<string, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-5 py-2.5 text-lg',
};

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({
    label,
    error,
    helperText,
    options,
    value,
    defaultValue,
    onChange,
    onFocus,
    onBlur,
    disabled = false,
    placeholder = 'Select an option',
    fullWidth = false,
    size = 'md',
    name,
    id,
    'aria-label': ariaLabel,
    'aria-invalid': ariaInvalid,
    className = '',
  }: SelectProps,
  ref,
) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className={`${fullWidth ? 'w-full' : ''}`}>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1.5">{label}</label>}
      <div className="relative">
        <motion.select
          ref={ref}
          disabled={disabled}
          value={value}
          defaultValue={defaultValue}
          onChange={(e) => onChange?.(e.target.value)}
          onFocus={onFocus}
          onBlur={(e) => {
            setIsOpen(false);
            onBlur?.(e);
          }}
          onClick={() => setIsOpen(!isOpen)}
          name={name}
          id={id}
          aria-label={ariaLabel}
          aria-invalid={ariaInvalid || !!error}
          className={`
            w-full appearance-none rounded-md border
            ${error ? 'border-error-500 focus:ring-error-500' : 'border-gray-300 focus:ring-primary-500'}
            ${sizeStyles[size]}
            focus:outline-none focus:ring-2 focus:ring-offset-2
            ${disabled ? 'opacity-50 cursor-not-allowed bg-gray-50' : 'bg-white'}
            pr-10
            ${className}
          `}
          initial={{ opacity: 0, x: -10 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.2 }}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((option) => (
            <option key={option.value} value={option.value} disabled={option.disabled}>
              {option.label}
            </option>
          ))}
        </motion.select>
        <ChevronDown
          className={`absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none transition-transform ${isOpen ? 'rotate-180' : ''}`}
          size={size === 'lg' ? 20 : 16}
        />
      </div>
      {helperText && <p className={`mt-1.5 text-sm ${error ? 'text-error-600' : 'text-gray-500'}`}>{helperText}</p>}
    </div>
  );
});

Select.displayName = 'Select';
