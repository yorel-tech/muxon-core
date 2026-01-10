'use client';

import { motion } from 'framer-motion';
import { forwardRef, type ReactNode, type ChangeEvent, type FocusEvent } from 'react';
import { Check } from 'lucide-react';

export interface CheckboxProps {
  label?: string;
  error?: string;
  helperText?: string;
  checked?: boolean;
  defaultChecked?: boolean;
  onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
  onFocus?: (event: FocusEvent<HTMLInputElement>) => void;
  onBlur?: (event: FocusEvent<HTMLInputElement>) => void;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  name?: string;
  id?: string;
  'aria-label'?: string;
  'aria-invalid'?: boolean;
  className?: string;
}

const sizeStyles: Record<string, string> = {
  sm: 'h-4 w-4',
  md: 'h-5 w-5',
  lg: 'h-6 w-6',
};

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({
    label,
    error,
    helperText,
    checked,
    defaultChecked,
    onChange,
    onFocus,
    onBlur,
    disabled = false,
    size = 'md',
    name,
    id,
    'aria-label': ariaLabel,
    'aria-invalid': ariaInvalid,
    className = '',
  }: CheckboxProps,
  ref,
) => {
    return (
    <div className={`${className}`}>
      <label className="flex items-start gap-3 cursor-pointer">
        <div className="relative flex items-center justify-center">
          <motion.input
            ref={ref}
            type="checkbox"
            checked={checked}
            defaultChecked={defaultChecked}
            onChange={onChange}
            onFocus={onFocus}
            onBlur={onBlur}
            disabled={disabled}
            name={name}
            id={id}
            aria-label={ariaLabel}
            aria-invalid={ariaInvalid || !!error}
            className={`
              appearance-none rounded border
              ${error ? 'border-error-500 focus:ring-error-500' : 'border-gray-300 focus:ring-primary-500'}
              ${sizeStyles[size]}
              focus:outline-none focus:ring-2 focus:ring-offset-2
              ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
              transition-all duration-200
              checked:bg-primary-500 checked:border-primary-500
            `}
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.2 }}
          />
          <Check
            className={`absolute pointer-events-none transition-all duration-200 ${
              checked ? 'text-white opacity-100' : 'text-white opacity-0'
            }`}
            size={size === 'lg' ? 14 : size === 'sm' ? 10 : 12}
          />
        </div>
        {label && (
          <div className="flex-1">
            <span className={`text-sm ${disabled ? 'text-gray-400' : 'text-gray-700'}`}>
              {label}
            </span>
            {helperText && (
              <p className={`mt-1 text-sm ${error ? 'text-error-600' : 'text-gray-500'}`}>
                {helperText}
              </p>
            )}
          </div>
        )}
      </label>
      {error && !label && <p className="mt-1.5 text-sm text-error-600">{error}</p>}
    </div>
  );
});

Checkbox.displayName = 'Checkbox';
