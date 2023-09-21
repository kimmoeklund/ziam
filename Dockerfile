FROM gcr.io/distroless/java17-debian11
ADD jvm/target/scala-3.3.0/ziam.jar /
ADD databases/ziam.db /databases/ziam.db
CMD ["ziam.jar"]
