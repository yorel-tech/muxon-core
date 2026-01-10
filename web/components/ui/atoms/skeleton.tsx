'use client';

import { motion } from 'framer-motion';
import { forwardRef } from 'react';
import { cn } from '@/lib/utils';

export interface SkeletonProps {
  variant?: 'default' | 'text' | 'circular' | 'rectangular';
  className?: string;
  width?: string;
  height?: string;
  count?: number;
  id?: string;
}

const variantClasses: Record<string, string> = {
  default: 'rounded-md',
  text: 'h-4 w-3/4 rounded',
  circular: 'rounded-full',
  rectangular: 'rounded-none',
};

export const Skeleton = forwardRef<HTMLDivElement, SkeletonProps>(
  ({ variant = 'default', className = '', width, height, count = 1, id }, ref,
) => {
  const getVariantClasses = () => {
    return variantClasses[variant] || variantClasses.default;
  };

  return (
    <>
      {Array.from({ length: count }).map((_, index) => (
        <motion.div
          key={index}
          ref={ref}
          className={cn(
            'animate-pulse bg-gray-200',
            getVariantClasses(),
            width || (variant === 'text' ? 'w-full' : 'h-4'),
            height || (variant === 'text' ? 'h-4' : 'w-full'),
            className,
          )}
          initial={{ opacity: 0.5 }}
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 0.5, repeat: Infinity, repeatType: 'reverse', ease: 'easeInOut' }}
          id={id}
        />
      ))}
    </>
  );
});

Skeleton.displayName = 'Skeleton';
