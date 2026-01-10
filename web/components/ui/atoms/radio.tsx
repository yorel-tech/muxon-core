'use client';

import { motion } from 'framer-motion';
import { forwardRef, type ReactNode, type ChangeEvent, type FocusEvent } from 'react';

export interface RadioOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface RadioProps {
  label?: string;
  error?: string;
  helperText?: string;
  options: RadioOption[];
  name: string;
  value?: string;
  defaultValue?: string;
  onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
  onFocus?: (event: FocusEvent<HTMLInputElement>) => void;
  onBlur?: (event: FocusEvent<HTMLInputElement>) => void;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  orientation?: 'vertical' | 'horizontal';
  'aria-label'?: string;
  'aria-invalid'?: boolean;
  className?: string;
}

const sizeStyles: Record<string, string> = {
  sm: 'h-4 w-4',
  md: 'h-5 w-5',
  lg: 'h-6 w-6',
};

export const Radio = forwardRef<HTMLDivElement, RadioProps>(
  ({
    label,
    error,
    helperText,
    options,
    name,
    value,
    defaultValue,
    onChange,
    onFocus,
    onBlur,
    disabled = false,
    size = 'md',
    orientation = 'vertical',
    'aria-label': ariaLabel,
    'aria-invalid': ariaInvalid,
    className = '',
  }: RadioProps,
  ref,
) => {
  return (
    <div className={`${className}`}>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1.5">{label}</label>}
      <div className={`${orientation === 'horizontal' ? 'flex items-center gap-6' : 'space-y-2'}`}>
        {options.map((option) => (
          <label
            key={option.value}
            className={`flex items-center gap-3 cursor-pointer ${disabled || option.disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            <div className="relative flex items-center justify-center">
              <motion.input
                type="radio"
                name={name}
                value={option.value}
                checked={value === option.value}
                defaultChecked={defaultValue === option.value}
                onChange={onChange}
                onFocus={onFocus}
                onBlur={onBlur}
                disabled={disabled || option.disabled}
                aria-label={option.label}
                aria-invalid={ariaInvalid || !!error}
                className={`
                  appearance-none rounded-full border
                  ${error ? 'border-error-500 focus:ring-error-500' : 'border-gray-300 focus:ring-primary-500'}
                  ${sizeStyles[size]}
                  focus:outline-none focus:ring-2 focus:ring-offset-2
                  ${disabled || option.disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                  transition-all duration-200
                  checked:bg-primary-500 checked:border-primary-500
                `}
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.2 }}
              />
              <div
                className={`absolute pointer-events-none transition-all duration-200 ${
                  value === option.value || defaultValue === option.value
                    ? 'bg-white opacity-100'
                    : 'bg-white opacity-0'
                }`}
                style={{
                  width: size === 'lg' ? '12px' : size === 'sm' ? '8px' : '10px',
                  height: size === 'lg' ? '12px' : size === 'sm' ? '8px' : '10px',
                }}
              />
            </div>
            <span
              className={`text-sm ${
                disabled || option.disabled ? 'text-gray-400' : 'text-gray-700'
              }`}
            >
              {option.label}
            </span>
          </label>
        ))}
      </div>
      {helperText && <p className={`mt-1.5 text-sm ${error ? 'text-error-600' : 'text-gray-500'}`}>{helperText}</p>}
      {error && !label && <p className="mt-1.5 text-sm text-error-600">{error}</p>}
    </div>
  );
});

Radio.displayName = 'Radio';
