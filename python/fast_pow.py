import matplotlib
import matplotlib.pyplot as plt

import numpy as np
import math

from scipy import optimize

def exp2_fit(x, a, b, c, d):
       result = a + b * x + c * x * x + d * x * x * x
       return result

def log2_fit(x, a, b, c, d):
       result = a + b * x + c * x * x+ d * x * x * x
       return result

point_count = 400

exp_points = np.arange(0, 1, 1 / point_count)
log_points = np.arange(0.5, 1, (1 - 0.5) / point_count)

exp2_ref_values = []
log2_ref_values = []

for index in range(point_count):
       exp2_ref_values.append(math.pow(2.0, exp_points[index]))
       log2_ref_values.append(math.log(log_points[index]) / math.log(2))

exp_params, exp_params_covariance = optimize.curve_fit(exp2_fit, exp_points, exp2_ref_values, p0=[1, 1, 1, 1])
log_params, log_params_covariance = optimize.curve_fit(log2_fit, log_points, log2_ref_values, p0=[1, 1, 1, 1])

print(float.hex(exp_params[0]))
print(float.hex(exp_params[1]))
print(float.hex(exp_params[2]))
print(float.hex(exp_params[3]))

print(float.hex(log_params[0]))
print(float.hex(log_params[1]))
print(float.hex(log_params[2]))
print(float.hex(log_params[3]))

exp2_fit_values = exp2_fit(exp_points, exp_params[0], exp_params[1], exp_params[2], exp_params[3])
log2_fit_values = log2_fit(log_points, log_params[0], log_params[1], log_params[2], log_params[3])

fig, ax = plt.subplots()
ax.plot(exp_points, exp2_ref_values)
ax.plot(exp_points, exp2_fit_values)

ax.grid()

fig.savefig("test.png")

fig, ax = plt.subplots()
ax.plot(log_points, log2_ref_values)
ax.plot(log_points, log2_fit_values)

ax.grid()

fig.savefig("test.png")

plt.show()
