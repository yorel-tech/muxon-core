'use client';

import { motion } from 'framer-motion';
import { Loader2 } from 'lucide-react';
import { forwardRef, type ReactNode, type MouseEvent, type FocusEvent } from 'react';

export interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  fullWidth?: boolean;
  disabled?: boolean;
  className?: string;
  children?: ReactNode;
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void;
  onFocus?: (event: FocusEvent<HTMLButtonElement>) => void;
  onBlur?: (event: FocusEvent<HTMLButtonElement>) => void;
  type?: 'button' | 'submit' | 'reset';
  name?: string;
  value?: string;
  id?: string;
  'aria-label'?: string;
  'aria-disabled'?: boolean;
}

const variantStyles: Record<string, string> = {
  primary: 'bg-primary-500 text-white hover:bg-primary-600 focus:ring-primary-500',
  secondary: 'bg-gray-100 text-gray-900 hover:bg-gray-200 focus:ring-gray-400',
  ghost: 'bg-transparent text-gray-700 hover:bg-gray-100 focus:ring-gray-400',
  danger: 'bg-error-500 text-white hover:bg-error-600 focus:ring-error-500',
};

const sizeStyles: Record<string, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      fullWidth = false,
      disabled = false,
      className = '',
      children,
      onClick,
      onFocus,
      onBlur,
      type = 'button',
      name,
      value,
      id,
      'aria-label': ariaLabel,
      'aria-disabled': ariaDisabled,
    }: ButtonProps,
    ref: React.Ref<HTMLButtonElement>
  ) => {
    const baseClasses = `
      inline-flex items-center justify-center gap-2 rounded-md font-medium
      transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2
      disabled:opacity-50 disabled:cursor-not-allowed
      ${variantStyles[variant]} ${sizeStyles[size]}
      ${fullWidth ? 'w-full' : ''}
      ${className}
    `;

    return (
      <motion.button
        ref={ref}
        className={baseClasses}
        disabled={disabled || isLoading}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        onClick={onClick}
        onFocus={onFocus}
        onBlur={onBlur}
        type={type}
        name={name}
        value={value}
        id={id}
        aria-label={ariaLabel}
        aria-disabled={ariaDisabled || disabled || isLoading}
      >
        {isLoading ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <>
            {leftIcon && <span className="flex items-center">{leftIcon}</span>}
            {children}
            {rightIcon && <span className="flex items-center">{rightIcon}</span>}
          </>
        )}
      </motion.button>
    );
  }
);

Button.displayName = 'Button';
