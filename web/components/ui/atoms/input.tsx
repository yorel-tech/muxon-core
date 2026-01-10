'use client';

import { motion } from 'framer-motion';
import { forwardRef, useState, type ReactNode, type ChangeEvent, type FocusEvent } from 'react';
import { AlertCircle, Eye, EyeOff, Search } from 'lucide-react';

export interface InputProps {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  fullWidth?: boolean;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'default' | 'search';
  showPasswordToggle?: boolean;
  disabled?: boolean;
  placeholder?: string;
  value?: string | number;
  defaultValue?: string | number;
  onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
  onFocus?: (event: FocusEvent<HTMLInputElement>) => void;
  onBlur?: (event: FocusEvent<HTMLInputElement>) => void;
  type?: 'text' | 'password' | 'email' | 'search' | 'number' | 'color';
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

const variantStyles: Record<string, string> = {
  default: 'border-gray-300 focus:ring-primary-500',
  search: 'border-transparent bg-gray-50 focus:ring-primary-500',
};

export const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      error,
      helperText,
      leftIcon,
      rightIcon,
      fullWidth = false,
      size = 'md',
      variant = 'default',
      showPasswordToggle = false,
      disabled = false,
      placeholder,
      value,
      defaultValue,
      onChange,
      onFocus,
      onBlur,
      type = 'text',
      name,
      id,
      'aria-label': ariaLabel,
      'aria-invalid': ariaInvalid,
      className = '',
    }: InputProps,
    ref
  ) => {
    const [showPassword, setShowPassword] = useState(false);

    return (
      <div className={`${fullWidth ? 'w-full' : ''}`}>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
              {leftIcon}
            </div>
          )}
          <motion.input
            ref={ref}
            type={showPasswordToggle && type === 'password' ? (showPassword ? 'text' : 'password') : type}
            disabled={disabled}
            placeholder={placeholder}
            value={value !== undefined ? String(value) : undefined}
            defaultValue={defaultValue !== undefined ? String(defaultValue) : undefined}
            onChange={onChange}
            onFocus={onFocus}
            onBlur={onBlur}
            name={name}
            id={id}
            aria-label={ariaLabel}
            aria-invalid={ariaInvalid || !!error}
            className={`
              w-full rounded-md border
              ${variantStyles[variant]}
              ${sizeStyles[size]}
              ${error ? 'border-error-500 focus:ring-error-500' : 'focus:ring-2 focus:ring-offset-2'}
              ${leftIcon ? 'pl-10' : 'pl-3'}
              ${rightIcon ? 'pr-10' : 'pr-3'}
              ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
              ${className}
            `}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.2 }}
          />
          {showPasswordToggle && type === 'password' && (
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          )}
          {variant === 'search' && !rightIcon && (
            <Search className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          )}
          {error && (
            <AlertCircle className="absolute right-3 top-1/2 -translate-y-1/2 text-error-500" size={18} />
          )}
        </div>
        {helperText && (
          <p className={`mt-1.5 text-sm ${error ? 'text-error-600' : 'text-gray-500'}`}>
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
