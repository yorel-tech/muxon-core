'use client';

import { motion } from 'framer-motion';
import { forwardRef, type ChangeEvent } from 'react';
import { cn } from '@/lib/utils';

export interface ProgressBarProps {
  value: number;
  max?: number;
  label?: string;
  showValue?: boolean;
  size?: 'sm' | 'md' | 'lg';
  color?: 'primary' | 'success' | 'warning' | 'error' | 'info';
  className?: string;
  onChange?: (value: number) => void;
}

const sizeClasses = {
  sm: 'h-1.5',
  md: 'h-2',
  lg: 'h-3',
};

const colorClasses = {
  primary: 'bg-primary-500',
  success: 'bg-success-500',
  warning: 'bg-warning-500',
  error: 'bg-error-500',
  info: 'bg-info-500',
};

export const ProgressBar = forwardRef<HTMLDivElement, ProgressBarProps>(
  (
    {
      value,
      max = 100,
      label,
      showValue = false,
      size = 'md',
      color = 'primary',
      className = '',
      onChange,
    },
    ref,
  ) => {
    const percentage = Math.min(100, Math.max(0, (value / max) * 100));

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
      const newValue = Number(event.target.value);
      if (!isNaN(newValue) && newValue >= 0 && newValue <= max) {
        onChange?.(newValue);
      }
    };

    return (
      <div ref={ref} className={cn('w-full', className)}>
        {(label || showValue) && (
          <div className="flex items-center justify-between mb-2">
            {label && <label className="text-sm font-medium text-gray-700">{label}</label>}
            {showValue && (
              <span className="text-sm font-medium text-gray-700">
                {Math.round(percentage)}%
              </span>
            )}
          </div>
        )}
        <div className="relative w-full bg-gray-200 rounded-full overflow-hidden">
          <motion.div
            className={cn('h-full rounded-full', colorClasses[color], sizeClasses[size])}
            initial={{ width: 0 }}
            animate={{ width: `${percentage}%` }}
            transition={{ duration: 0.3, ease: 'easeOut' }}
          />
          <input
            type="range"
            min={0}
            max={max}
            value={value}
            onChange={handleChange}
            className={cn(
              'absolute inset-0 w-full h-full opacity-0 cursor-pointer',
              'focus:opacity-100',
            )}
            aria-label={label || 'Progress bar'}
            aria-valuenow={value}
            aria-valuemax={max}
          />
        </div>
      </div>
    );
  },
);

ProgressBar.displayName = 'ProgressBar';
