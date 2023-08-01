FROM gcr.io/distroless/java17-debian11
COPY jvm/target/scala-3.3.0/ziam.jar /
COPY databases/ziam.db /
CMD ["ziam.jar"]
