package client.service;

import client.model.TaskDTO;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CSVUtil {

    private static final Set<String> VALID_STATUS = Set.of("TODO", "IN_PROGRESS", "DONE");
    private static final Set<String> VALID_PRIORITY = Set.of("LOW", "MEDIUM", "HIGH");

    public static void exportTasksToCSV(List<TaskDTO> tasks, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("ID,Tieu de,Nhom,Trang thai,Do uu tien,Han chot,Nguoi thuc hien,Mo ta");

            for (TaskDTO task : tasks) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    task.getId(),
                    escapeCsv(task.getTitle()),
                    escapeCsv(task.getGroupName()),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDueDate(),
                    escapeCsv(task.getAssigneeName()),
                    escapeCsv(task.getDescription())
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Loi khi xuat file CSV: " + e.getMessage());
        }
    }

    public static List<TaskDTO> importTasksFromCSV(String filePath) {
        List<TaskDTO> tasks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();
            if (header == null) {
                throw new RuntimeException("File CSV rong!");
            }
            if (!header.toLowerCase().contains("tieu de") && !header.toLowerCase().contains("title")) {
                throw new RuntimeException("File CSV sai dinh dang header! Can co cot: ID,Tieu de,Nhom,Trang thai,Do uu tien,Han chot,Nguoi thuc hien,Mo ta");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                String[] values = parseCsvLine(line);
                if (values.length < 6) {
                    throw new RuntimeException("Dong " + lineNumber + " khong du cot du lieu!");
                }

                TaskDTO task = new TaskDTO();
                if (!values[0].trim().isEmpty()) {
                    try {
                        task.setId(Integer.parseInt(values[0].trim()));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Dong " + lineNumber + ": ID khong hop le!");
                    }
                }

                task.setTitle(values[1].trim());
                if (task.getTitle().isEmpty()) {
                    throw new RuntimeException("Dong " + lineNumber + ": Tieu de khong duoc de trong!");
                }

                task.setGroupName(values[2].trim());
                task.setStatus(values[3].trim().toUpperCase());
                task.setPriority(values[4].trim().toUpperCase());
                task.setDueDate(values[5].trim());

                if (!VALID_STATUS.contains(task.getStatus())) {
                    throw new RuntimeException("Dong " + lineNumber + ": Trang thai phai la TODO, IN_PROGRESS hoac DONE!");
                }
                if (!VALID_PRIORITY.contains(task.getPriority())) {
                    throw new RuntimeException("Dong " + lineNumber + ": Do uu tien phai la LOW, MEDIUM hoac HIGH!");
                }
                if (!task.getDueDate().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    throw new RuntimeException("Dong " + lineNumber + ": Han chot phai co dinh dang yyyy-MM-dd!");
                }

                if (values.length > 6) {
                    task.setAssigneeName(values[6].trim());
                }
                if (values.length > 7) {
                    task.setDescription(values[7].trim());
                }

                tasks.add(task);
            }
        } catch (IOException e) {
            throw new RuntimeException("Loi khi doc file CSV: " + e.getMessage());
        }

        if (tasks.isEmpty()) {
            throw new RuntimeException("File CSV khong co du lieu hop le!");
        }

        return tasks;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }
}