'use client';

import { motion } from 'framer-motion';
import { forwardRef } from 'react';

export interface BadgeProps {
  variant?: 'default' | 'success' | 'warning' | 'error' | 'info' | 'nexus';
  size?: 'sm' | 'md' | 'lg';
  children: React.ReactNode;
  dot?: boolean;
}

const variantStyles: Record<string, string> = {
  default: 'bg-gray-100 text-gray-700',
  success: 'bg-success-100 text-success-700',
  warning: 'bg-warning-100 text-warning-700',
  error: 'bg-error-100 text-error-700',
  info: 'bg-info-100 text-info-700',
  nexus: 'bg-nexus-100 text-nexus-700',
};

const sizeStyles: Record<string, string> = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-0.5 text-sm',
  lg: 'px-3 py-1 text-base',
};

export const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ variant = 'default', size = 'md', children, dot = false }, ref) => {
    return (
      <motion.span
        ref={ref}
        className={`
          inline-flex items-center gap-1.5 rounded-full font-medium
          ${variantStyles[variant]}
          ${sizeStyles[size]}
        `}
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.2 }}
      >
        {children}
        {dot && (
          <span className="w-1.5 h-1.5 rounded-full bg-current" />
        )}
      </motion.span>
    );
  }
);

Badge.displayName = 'Badge';
