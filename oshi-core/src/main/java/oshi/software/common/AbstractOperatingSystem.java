/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.common;

import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;

@SuppressWarnings("deprecation")
public abstract class AbstractOperatingSystem implements OperatingSystem {

    private final Supplier<String> manufacturer = memoize(this::queryManufacturer);
    private final Supplier<FamilyVersionInfo> familyVersionInfo = memoize(this::queryFamilyVersionInfo);
    // Initialize based on JVM Bitness. Individual OS implementations will test
    // if 32-bit JVM running on 64-bit OS
    protected final int jvmBitness = System.getProperty("os.arch").indexOf("64") != -1 ? 64 : 32;
    private final Supplier<Integer> bitness = memoize(this::queryBitness);
    // Test if sudo or admin privileges: 1 = unknown, 0 = no, 1 = yes
    private final Supplier<Boolean> elevated = memoize(this::queryElevated);

    protected OperatingSystemVersion version;

    /*
     * Comparators for use in processSort().
     */
    private static final Comparator<OSProcess> CPU_DESC_SORT = Comparator
            .comparingDouble(OSProcess::calculateCpuPercent).reversed();

    private static final Comparator<OSProcess> RSS_DESC_SORT = Comparator.comparingLong(OSProcess::getResidentSetSize)
            .reversed();

    private static final Comparator<OSProcess> UPTIME_ASC_SORT = Comparator.comparingLong(OSProcess::getUpTime);

    private static final Comparator<OSProcess> UPTIME_DESC_SORT = UPTIME_ASC_SORT.reversed();

    private static final Comparator<OSProcess> PID_ASC_SORT = Comparator.comparingInt(OSProcess::getProcessID);

    private static final Comparator<OSProcess> PARENTPID_ASC_SORT = Comparator
            .comparingInt(OSProcess::getParentProcessID);

    private static final Comparator<OSProcess> NAME_ASC_SORT = Comparator.comparing(OSProcess::getName,
            String.CASE_INSENSITIVE_ORDER);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    protected abstract String queryManufacturer();

    @Override
    public String getFamily() {
        return familyVersionInfo.get().family;
    }

    @Override
    public OSVersionInfo getVersionInfo() {
        return familyVersionInfo.get().versionInfo;
    }

    protected abstract FamilyVersionInfo queryFamilyVersionInfo();

    @Override
    public OperatingSystemVersion getVersion() {
        return this.version;
    }

    @Override
    public int getBitness() {
        return bitness.get();
    }

    protected abstract int queryBitness();

    @Override
    public boolean isElevated() {
        return elevated.get();
    }

    protected abstract boolean queryElevated();

    /**
     * Sorts an array of processes using the specified sorting, returning an array
     * with the top limit results if positive.
     *
     * @param processes
     *            The array to sort
     * @param limit
     *            The number of results to return if positive; if zero returns all
     *            results
     * @param sort
     *            The sorting to use, or null
     * @return An array of size limit (if positive) or of all processes, sorted as
     *         specified
     */
    protected List<OSProcess> processSort(List<OSProcess> processes, int limit, ProcessSort sort) {
        if (sort != null) {
            switch (sort) {
            case CPU:
                processes.sort(CPU_DESC_SORT);
                break;
            case MEMORY:
                processes.sort(RSS_DESC_SORT);
                break;
            case OLDEST:
                processes.sort(UPTIME_DESC_SORT);
                break;
            case NEWEST:
                processes.sort(UPTIME_ASC_SORT);
                break;
            case PID:
                processes.sort(PID_ASC_SORT);
                break;
            case PARENTPID:
                processes.sort(PARENTPID_ASC_SORT);
                break;
            case NAME:
                processes.sort(NAME_ASC_SORT);
                break;
            default:
                // Should never get here! If you get this exception you've
                // added something to the enum without adding it here. Tsk.
                throw new IllegalArgumentException("Unimplemented enum type: " + sort.toString());
            }
        }
        // Return max of limit or process size
        // Nonpositive limit means return all
        int maxProcs = processes.size();
        if (limit > 0 && maxProcs > limit) {
            maxProcs = limit;
        } else {
            return processes;
        }
        List<OSProcess> procs = new ArrayList<>();
        for (int i = 0; i < maxProcs; i++) {
            procs.add(processes.get(i));
        }
        return procs;
    }

    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        return getProcesses(limit, sort, false);
    }

    @Override
    public List<OSProcess> getProcesses(Collection<Integer> pids) {
        List<OSProcess> returnValue = new ArrayList<>(pids.size());
        for (Integer pid : pids) {
            OSProcess process = getProcess(pid);
            if (process != null) {
                returnValue.add(process);
            }
        }
        return returnValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getManufacturer()).append(' ').append(getFamily()).append(' ').append(getVersion().toString());
        return sb.toString();
    }

    protected static final class FamilyVersionInfo {
        private final String family;
        private final OSVersionInfo versionInfo;

        public FamilyVersionInfo(String family, OSVersionInfo versionInfo) {
            this.family = family;
            this.versionInfo = versionInfo;
        }
    }
}
