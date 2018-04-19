final String STMT = ...;
try (PreparedStatement p = c.prepareStatement(STMT)) {
  // check parameters e.g.
  // if (username == null || username.isEmpty()) {
  //  return Result.failure("getPersonView: username cannot be empty");
  // }

  // do stuff

  // return Result.success(map);
} (catch SQLException e) {
  return Result.fatal(e.getMessage());
}
