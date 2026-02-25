import { motion } from "motion/react";

interface MatrixVisualizationProps {
  size?: number;
  delay?: number;
}

export function MatrixVisualization({ size = 4, delay = 0 }: MatrixVisualizationProps) {
  const generateMatrix = () => {
    const matrix = [];
    for (let i = 0; i < size; i++) {
      const row = [];
      for (let j = 0; j < size; j++) {
        // Create tridiagonal-like pattern
        if (Math.abs(i - j) <= 1) {
          if (i === j) row.push('a');
          else if (i < j) row.push('b');
          else row.push('c');
        } else {
          row.push('0');
        }
      }
      matrix.push(row);
    }
    return matrix;
  };

  const matrix = generateMatrix();

  return (
    <div className="inline-flex flex-col gap-1">
      <div className="flex items-center gap-1">
        <motion.div
          className="w-1 h-full bg-gray-300 rounded-full"
          initial={{ scaleY: 0 }}
          animate={{ scaleY: 1 }}
          transition={{ duration: 0.4, delay: delay }}
        />
        <div className="flex flex-col gap-1">
          {matrix.map((row, i) => (
            <div key={i} className="flex gap-1">
              {row.map((cell, j) => (
                <motion.div
                  key={`${i}-${j}`}
                  className={`w-10 h-10 flex items-center justify-center rounded-md text-sm font-mono ${
                    cell === '0'
                      ? 'bg-gray-50 text-gray-300'
                      : 'bg-blue-50 text-blue-600 font-medium'
                  }`}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{
                    duration: 0.3,
                    delay: delay + 0.1 + (i + j) * 0.02,
                  }}
                >
                  {cell}
                </motion.div>
              ))}
            </div>
          ))}
        </div>
        <motion.div
          className="w-1 h-full bg-gray-300 rounded-full"
          initial={{ scaleY: 0 }}
          animate={{ scaleY: 1 }}
          transition={{ duration: 0.4, delay: delay }}
        />
      </div>
    </div>
  );
}
